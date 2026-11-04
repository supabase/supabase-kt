package io.github.jan.supabase.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.jwt.JWTUtils
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.buildUrl
import io.github.jan.supabase.collections.AtomicMutableMap
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.logging.createLogger
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.e
import io.github.jan.supabase.logging.i
import io.github.jan.supabase.logging.w
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import io.github.jan.supabase.realtime.broadcast.decodeBinaryPayload
import io.github.jan.supabase.realtime.broadcast.decodeV2Text
import io.github.jan.supabase.realtime.websocket.KtorRealtimeWebsocketFactory
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocket
import io.github.jan.supabase.supabaseJson
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration

@PublishedApi internal class RealtimeImpl(override val supabaseClient: SupabaseClient, override val config: Realtime.Config) : Realtime {

    override val logger: SupabaseLogger = supabaseClient.createLogger(Realtime.LOGGING_TAG, config)
    private val websocketFactory = config.websocketFactory ?: KtorRealtimeWebsocketFactory(supabaseClient.httpClient.httpClient)
    private var _websocket: AtomicReference<RealtimeWebsocket?> = AtomicReference(null)
    override val websocket: RealtimeWebsocket?
        get() = _websocket.load()
    @Suppress("MagicNumber")
    private val _status = MutableStateFlow(Realtime.Status.DISCONNECTED)
    override val status: StateFlow<Realtime.Status> = _status.asStateFlow()
    private val _subscriptions = AtomicMutableMap<String, RealtimeChannel>()
    override val subscriptions: Map<String, RealtimeChannel> = _subscriptions
    internal val scope = config.coroutineScope ?: CoroutineScope(
        supabaseClient.coroutineDispatcher +
                SupervisorJob() +
                CoroutineExceptionHandler { _, throwable ->
                    if (throwable !is CancellationException) {
                        logger.e(throwable) { "Unhandled exception in Realtime scope. Reconnecting..." }
                        reconnect()
                    }
                }
    )
    private val mutex = Mutex()
    private val isReconnecting = AtomicBoolean(false)
    private val _accessToken = AtomicReference<String?>(null)
    val accessToken get() = _accessToken.load()
    private var heartbeatJob: Job? = null
    private var messageJob: Job? = null
    private var disconnectJob: Job? = null
    private val heartbeatRef = AtomicInt(NO_HEARTBEAT)
    override val apiVersion: Int
        get() = Realtime.API_VERSION

    override val pluginKey: String
        get() = Realtime.key

    override var serializer = config.serializer ?: supabaseClient.defaultSerializer
    private val websocketUrl = realtimeWebsocketUrl()
    private val incrementId = AtomicInt(0)

    init {
        if(config.customAccessTokenProvider) {
            logger.w { "You are setting a custom access token provider. This can lead to unexpected behavior." }
        }
    }

    override suspend fun connect() = connect(false)

    private suspend fun connect(reconnect: Boolean): Unit = mutex.withLock {
        if (reconnect) {
            delay(config.reconnectDelay)
            logger.d { "Reconnecting..." }
        }
        if (status.value == Realtime.Status.CONNECTED) return
        _status.value = Realtime.Status.CONNECTING
        try {
            _websocket.store(websocketFactory.create(websocketUrl))
            heartbeatRef.store(NO_HEARTBEAT)
            _status.value = Realtime.Status.CONNECTED
            logger.i { "Connected to realtime websocket!" }
            listenForMessages()
            startHeartbeating()
            if(reconnect) {
                rejoinChannels()
                isReconnecting.store(false)
            }
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.w(e) { """
                Error while trying to connect to realtime websocket. Trying again in ${config.reconnectDelay}
                URL: $websocketUrl
                """.trimIndent() }
            if(reconnect) {
                isReconnecting.store(false)
            }
            reconnect()
        }
    }

    override fun addChannel(channel: RealtimeChannel) {
        cancelPendingDisconnect()
        _subscriptions[channel.topic] = channel
    }

    override fun init() {
        if(config.customAccessTokenProvider) return
        scope.launch {
            supabaseClient.pluginManager.getPluginOrNull(Auth)?.sessionStatus?.collect { sessionStatus ->
                when (sessionStatus) {
                    is SessionStatus.Authenticated -> {
                        setAuth(sessionStatus.session.accessToken)
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (config.disconnectOnSessionLoss && status.value == Realtime.Status.CONNECTED) {
                            logger.w { "No auth session found, disconnecting from realtime websocket" }
                            disconnect()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun rejoinChannels() {
        scope.launch {
            val auth = supabaseClient.pluginManager.getPluginOrNull(Auth)

            // Wait for Auth to be initialized (so realtime doesn't think there is no token because its called earlier)
            if (auth != null && !config.customAccessTokenProvider) auth.sessionStatus.first { it !is SessionStatus.Initializing }
            setAuth()
            for (channel in _subscriptions.values) {
                if ((channel as? RealtimeChannelImpl)?.joinRequested?.load() == false) continue
                channel.subscribe()
            }
        }
    }

    private fun listenForMessages() {
        messageJob = scope.launch {
            try {
                _websocket.load()?.let {
                    while(it.hasIncomingMessages) {
                        val frame = it.receive() ?: return@launch
                        when(frame) {
                            is Frame.Binary -> {
                                if(config.vsn == RealtimeProtocolVersion.V1) {
                                    logger.w { "Received binary payload, but vsn is 1.0.0; binary frames are only supported in 2.0.0" }
                                    continue;
                                }
                                val broadcast = frame.readBytes().decodeBinaryPayload()
                                onBroadcast(broadcast)
                            }
                            is Frame.Text -> {
                                when(config.vsn) {
                                    RealtimeProtocolVersion.V1 -> onMessage(supabaseJson.decodeFromString(frame.readText()))
                                    RealtimeProtocolVersion.V2 -> onMessage(frame.readText().decodeV2Text())
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            } catch(e: Exception) {
                currentCoroutineContext().ensureActive()
                logger.w(e) { "Error while listening for messages. Trying again in ${config.reconnectDelay}" }
                reconnect()
            }
        }
    }

    private fun onBroadcast(broadcast: RealtimeBroadcast) {
        val channel = subscriptions[broadcast.topic] as? RealtimeChannelImpl
        logger.d { "Received ${broadcast::class}" }
        channel?.onBroadcast(broadcast)
    }

    private fun startHeartbeating() {
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(config.heartbeatInterval)
                if(!isActive) break
                launch {
                    sendHeartbeat()
                }
            }
        }
    }

    override fun disconnect() {
        logger.d { "Closing websocket connection" }
        messageJob?.cancel()
        heartbeatJob?.cancel()
        _websocket.load()?.disconnect()
        _websocket.store(null)
        for ((_, channel) in subscriptions) {
            channel.updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
        }
        _status.value = Realtime.Status.DISCONNECTED
    }

    override fun channel(channelId: String, builder: RealtimeChannelBuilder): RealtimeChannel {
        val topic = RealtimeTopic.withChannelId(channelId)
        if(subscriptions.containsKey(topic)) return subscriptions[topic]!!
        val channel = builder.build(this)
        addChannel(channel)
        return channel
    }

    private suspend fun onMessage(message: RealtimeMessage) {
        logger.d { "Received message $message" }
        val channel = subscriptions[message.topic] as? RealtimeChannelImpl
        val ref = message.ref?.toIntOrNull()
        if(message.topic == HEARTBEAT_TOPIC && ref != null && heartbeatRef.compareAndSet(ref, NO_HEARTBEAT)) {
            logger.d { "Heartbeat received" }
        } else {
            logger.d { "Received event ${message.event} for channel ${channel?.topic}" }
            channel?.onMessage(message)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun setAuth(token: String?) {
        val newToken = token ?: config.accessToken(supabaseClient)

        if(newToken != null) {
            val exp = JWTUtils.decodeJwt(newToken).claimsResponse.claims.exp ?: error("exp claim missing")
            val now = Clock.System.now()
            val diff = exp - now
            if(diff.isNegative()) {
                logger.w { "Token is expired. Not sending it to realtime." }
                return
            }
        }
        this._accessToken.store(newToken)
        if(status.value != Realtime.Status.CONNECTED) return
        scope.launch {
            subscriptions.values.filter { it.status.value == RealtimeChannel.Status.SUBSCRIBED }.forEach { it.updateAuth(accessToken) }
        }
    }

    private suspend fun sendHeartbeat() {
        if (heartbeatRef.load() != NO_HEARTBEAT) {
            heartbeatRef.store(NO_HEARTBEAT)
            logger.w { "Heartbeat timeout. Trying to reconnect in ${config.reconnectDelay}" }
            reconnect()
            return
        }
        val websocket = _websocket.load() ?: run {
            logger.d { "Skipping heartbeat, websocket is not connected" }
            return
        }
        logger.d { "Sending heartbeat" }
        val ref = websocket.makeRef()
        heartbeatRef.store(ref.toInt())
        send(RealtimeMessage(HEARTBEAT_TOPIC, "heartbeat", buildJsonObject { }, ref))
    }

    override suspend fun removeChannel(channel: RealtimeChannel) {
        if(channel.status.value in JOINED_OR_JOINING) {
            channel.unsubscribe()
        }
        _subscriptions.remove(channel.topic)
        if(subscriptions.isEmpty() && config.disconnectOnNoSubscriptions) {
            schedulePendingDisconnect()
        }
    }

    private fun schedulePendingDisconnect() {
        disconnectJob?.cancel()
        if(config.disconnectDelay != Duration.ZERO) {
            disconnectJob = scope.launch {
                delay(config.disconnectDelay)
                logger.d { "Scheduled disconnect fired. Disconnecting from realtime websocket..." }
                disconnect()
            }
            logger.d { "Scheduling to disconnect from realtime websocket in ${config.disconnectDelay}..." }
        } else {
            logger.d { "Disconnecting immediately from realtime websocket as delay set to zero..." }
            disconnect()
        }
    }

    private fun cancelPendingDisconnect() {
        if(disconnectJob == null) return
        logger.d { "Pending disconnect cancelled - channel activity detected" }
        disconnectJob?.cancel()
        disconnectJob = null
    }

    override suspend fun removeAllChannels() {
        _subscriptions.forEach { (_, it) ->
            if(it.status.value in JOINED_OR_JOINING) {
                it.unsubscribe()
            }
        }
        _subscriptions.clear()
        if(config.disconnectOnNoSubscriptions) {
            logger.d { "No more subscriptions, disconnecting from realtime websocket." }
            disconnect()
        }
    }

    override suspend fun close() {
        disconnect()
        scope.cancel()
        for ((_, channel) in subscriptions) {
            channel.teardown()
        }
    }

    override suspend fun block() {
        _websocket.load()?.blockUntilDisconnect() ?: error("No connection available")
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        return UnknownRestException("Unknown error in realtime plugin", response)
    }

    private fun realtimeBaseUrl(): String {
        val secure = config.secure ?: supabaseClient.useHTTPS
        val prefix = if (secure) "wss://" else "ws://"
        val baseUrl = config.customUrl
            ?: (prefix + supabaseClient.supabaseUrl)
        return buildUrl(baseUrl) {
            path("/realtime/v${Realtime.API_VERSION}")
        }
    }

    private fun realtimeWebsocketUrl(): String {
        return buildUrl(realtimeBaseUrl()) {
            parameters["apikey"] = supabaseClient.supabaseKey
            parameters["vsn"] = config.vsn.value
            pathSegments += listOf("websocket")
        }
    }

    fun broadcastUrl(): String {
        val secure = config.secure ?: supabaseClient.useHTTPS
        return buildUrl(realtimeBaseUrl()) {
            protocol = if(secure) URLProtocol.HTTPS else URLProtocol.HTTP
            pathSegments += listOf("api", "broadcast")
        }
    }

    override suspend fun send(message: RealtimeMessage) {
        try {
            _websocket.load()?.send(message, config.vsn)
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.w(e) { "Error while sending message $message. Reconnecting in ${config.reconnectDelay}" }
            reconnect()
        }
    }

    override suspend fun send(data: ByteArray) {
        try {
            _websocket.load()?.send(data)
        } catch(e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.e(e) { "Error while sending binary data. Reconnecting in ${config.reconnectDelay}" }
            reconnect()
        }
    }

    fun nextIncrementId(): Int {
        return incrementId.fetchAndIncrement()
    }

    private fun reconnect() {
        if(isReconnecting.compareAndSet(expectedValue = false, newValue = true)) {
            scope.launch {
                disconnect()
                connect(true)
            }
        }
    }

    private companion object {
        const val HEARTBEAT_TOPIC = "phoenix"
        // Not 0: refs start at 0, so a socket's first heartbeat can have ref 0
        const val NO_HEARTBEAT = -1
        val JOINED_OR_JOINING = setOf(RealtimeChannel.Status.SUBSCRIBED, RealtimeChannel.Status.SUBSCRIBING)
    }

}
