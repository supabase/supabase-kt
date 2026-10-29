package io.github.jan.supabase.realtime

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.collections.AtomicMutableList
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.e
import io.github.jan.supabase.logging.i
import io.github.jan.supabase.logging.w
import io.github.jan.supabase.putJsonObject
import io.github.jan.supabase.realtime.broadcast.BroadcastPayload
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import io.github.jan.supabase.realtime.broadcast.encodeBroadcast
import io.github.jan.supabase.realtime.event.RealtimeEvent
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocket
import io.github.jan.supabase.safeBody
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.headers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class RealtimeChannelImpl(
    override val realtime: Realtime,
    override val topic: String,
    private val broadcastJoinConfig: BroadcastJoinConfig,
    private val presenceJoinConfig: PresenceJoinConfig,
    private val isPrivate: Boolean,
    private val postgresChangesOptions: PostgresChangesOptions? = null,
) : RealtimeChannel {

    override val logger: SupabaseLogger = realtime.logger.appendTag(" [$topic$]")
    private val realtimeImpl: RealtimeImpl = realtime as RealtimeImpl
    private val clientChanges = AtomicMutableList<PostgresJoinConfig>()
    @SupabaseInternal
    override val callbackManager = CallbackManagerImpl(realtimeImpl.serializer)
    private val _status = MutableStateFlow(RealtimeChannel.Status.UNSUBSCRIBED)
    override val status = _status.asStateFlow()
    override val supabaseClient = realtimeImpl.supabaseClient

    private val broadcastUrl = realtimeImpl.broadcastUrl()
    private val subTopic = topic.replaceFirst(Regex("^${RealtimeTopic.PREFIX}:", RegexOption.IGNORE_CASE), "")
    private val httpClient = realtimeImpl.supabaseClient.httpClient
    private val userPresenceEnabled = presenceJoinConfig.enabled

    private val joinAttempt = AtomicInt(0)
    private val joinRef = AtomicReference<String?>(null)
    private val rejoinJob = AtomicReference<Job?>(null)
    private val joinTimeoutJob = AtomicReference<Job?>(null)
    // Why the channel stopped trying to join (it gave up or was closed), which ends a blocking subscribe()
    private val joinEnded = MutableStateFlow<JoinEnded?>(null)

    private suspend fun accessToken() = realtimeImpl.config.accessToken(supabaseClient) ?: realtimeImpl.accessToken

    private fun shouldEnablePresence(): Boolean =
        userPresenceEnabled || callbackManager.hasPresenceCallback()

    override fun teardown() {
        updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
        callbackManager.reset()
    }

    override suspend fun subscribe(blockUntilSubscribed: Boolean) {
        cancelRejoin()
        joinEnded.value = null
        join(blockUntilSubscribed)
    }

    @OptIn(SupabaseInternal::class)
    private suspend fun join(blockUntilSubscribed: Boolean) {
        if(realtimeImpl.status.value != Realtime.Status.CONNECTED) {
            if(!realtimeImpl.config.connectOnSubscribe) error("You can't subscribe to a channel while the realtime client is not connected. Did you forget to call `realtime.connect()`?")
            realtimeImpl.connect()
            // If connect fails, it will schedule a retry, wait for the connection
            realtimeImpl.status.first { it == Realtime.Status.CONNECTED }
        }
        if(!realtimeImpl.subscriptions.containsKey(topic)) {
            realtime.addChannel(this)
        }
        val initialWebsocket = realtime.websocket
        if(checkWebsocket(initialWebsocket, "Not connected to websocket while trying to subscribe")) return
        joinRef.store(initialWebsocket.makeRef())
        _status.value = RealtimeChannel.Status.SUBSCRIBING
        logger.d { "Subscribing to channel $topic" }
        val currentJwt = try {
            accessToken()
        } catch(t: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(t) { "Error occurred while fetching access token" }
            if(realtime.config.requireValidSession) {
                scheduleRejoin()
                return
            }
            null
        }
        val postgrestChanges = clientChanges.toList()
        presenceJoinConfig.enabled = shouldEnablePresence()
        val joinConfig = RealtimeJoinPayload(RealtimeJoinConfig(broadcastJoinConfig, presenceJoinConfig, postgrestChanges, isPrivate, postgresChangesOptions))
        val joinConfigObject = buildJsonObject {
            putJsonObject(Json.encodeToJsonElement(joinConfig).jsonObject)
            currentJwt?.let {
                put("access_token", currentJwt)
            }
        }
        logger.d { "Subscribing to channel with body $joinConfigObject" }
        val websocket = realtime.websocket
        if(checkWebsocket(websocket, "Not connected to websocket while trying to subscribe")) return
        realtimeImpl.send(
            RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_JOIN, joinConfigObject, websocket.makeRef(), joinRef.load())
        )
        startJoinTimeout()
        if(blockUntilSubscribed) {
            // Both are state, so an outcome that comes before this starts waiting isn't missed
            combine(status, joinEnded) { status, ended ->
                when {
                    status == RealtimeChannel.Status.SUBSCRIBED -> true
                    ended != null -> throw RealtimeChannelJoinException(ended.message, ended.error)
                    else -> false
                }
            }.first { it }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkWebsocket(websocket: RealtimeWebsocket?, message: String): Boolean {
        contract { returns(false) implies (websocket != null) }
        if(websocket == null) {
            logger.i { "$message. Aborting and unsubscribing from channel $topic..." }
            _status.value = RealtimeChannel.Status.UNSUBSCRIBED
            return true
        }
        return false
    }

    @OptIn(SupabaseInternal::class)
    suspend fun onMessage(message: RealtimeMessage) {
        // Ignore messages for a previous join, like Phoenix's client
        if(message.joinRef != null && message.joinRef != joinRef.load()) {
            logger.d { "Dropping ${message.event} for earlier join ${message.joinRef} of channel $topic" }
            return
        }
        val event = RealtimeEvent.resolveEvent(message)
        if(event == null) {
            logger.e { "Received message without event: $message" }
            return
        }
        event.handle(this, message)
    }

    fun onBroadcast(broadcast: RealtimeBroadcast) {
        callbackManager.triggerBroadcast(broadcast)
    }

    override suspend fun httpSend(event: String, payload: BroadcastPayload, builder: HttpSendBuilder.() -> Unit) {
        val token = accessToken()
        val builder = HttpSendBuilder().apply(builder)
        val response = httpClient.post(
            url = broadcastUrl,
        ) {
            headers {
                append("apikey", realtimeImpl.supabaseClient.supabaseKey)
                token?.let {
                    set("Authorization", "Bearer $it")
                }
            }
            when(payload) {
                is BroadcastPayload.Binary -> {
                    contentType(ContentType.Application.OctetStream)
                    setBody(payload.data)
                }
                is BroadcastPayload.Json -> {
                    contentType(ContentType.Application.Json)
                    setBody(payload.value)
                }
            }
            url.appendPathSegments(subTopic, "events", event)
            if(isPrivate) {
                parameter("private", true)
            }
            builder.timeout?.inWholeMilliseconds?.let {
                timeout {
                    requestTimeoutMillis = it
                }
            }
        }
        when(response.status) {
            HttpStatusCode.Accepted -> return
            HttpStatusCode.NotFound -> error("""
                httpSend() requires Realtime server v2.97.0 or newer; the endpoint returned 404. 
                Update your Supabase CLI to a recent version, or upgrade the Realtime server in your self-hosted setup.
            """.trimIndent())
        }
        val errorMessage = try {
            val body = response.safeBody<JsonObject>()
            body["error"]?.jsonPrimitive?.contentOrNull ?: body["message"]?.jsonPrimitive?.contentOrNull
        } catch(e: Exception) {
            logger.d(e) { "Exception thrown while decoding the error message from a httpSend response" }
            null
        } ?:  response.status.description
        throw RestException(errorMessage, null, response)
    }

    override fun scheduleRejoin() {
        // The join failed, so it can't also time out
        cancelJoinTimeout()
        logger.d { "Rejoining channel $topic in ${realtime.config.rejoinDelay}" }
        val job = realtimeImpl.scope.launch(start = CoroutineStart.LAZY) {
            delay(realtime.config.rejoinDelay)
            resubscribe()
        }
        rejoinJob.exchange(job)?.cancel()
        job.start()
    }

    private fun cancelRejoin() {
        rejoinJob.exchange(null)?.cancel()
    }

    // Reports the error, then rejoins until maxAttempts is reached
    internal fun rejoinAfter(error: RealtimeChannelError) {
        callbackManager.triggerError(error)
        val currentAttempt = joinAttempt.incrementAndFetch()
        if(currentAttempt >= realtime.config.maxAttempts) {
            logger.e { "Failed to rejoin channel $topic after $currentAttempt attempts. Giving up." }
            endJoin("Failed to join channel $topic after $currentAttempt attempts: ${error.reason ?: error.type}", error)
            updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
            return
        }
        scheduleRejoin()
    }

    internal fun endJoin(message: String, error: RealtimeChannelError?) {
        joinEnded.value = JoinEnded(message, error)
    }

    private fun startJoinTimeout() {
        val timeout = joinTimeout()
        val job = realtimeImpl.scope.launch(start = CoroutineStart.LAZY) {
            delay(timeout)
            if(status.value != RealtimeChannel.Status.SUBSCRIBING) return@launch
            logger.e { "No reply to the join of channel $topic within $timeout" }
            rejoinAfter(RealtimeChannelError(RealtimeChannelError.Type.TIMED_OUT, null, null))
        }
        joinTimeoutJob.exchange(job)?.cancel()
        job.start()
    }

    private fun cancelJoinTimeout() {
        joinTimeoutJob.exchange(null)?.cancel()
    }

    internal fun joinTimeout(): Duration {
        val options = postgresChangesOptions
        if(options?.wait != true || clientChanges.isEmpty()) return realtime.config.timeout
        // Like realtime-js: outlast the server holding the join until the postgres changes subscription is set up
        return maxOf(realtime.config.timeout, (options.timeout ?: POSTGRES_CHANGES_WAIT_TIMEOUT) + POSTGRES_CHANGES_WAIT_ERROR_GRACE)
    }

    override suspend fun unsubscribe() {
        updateStatus(RealtimeChannel.Status.UNSUBSCRIBING)
        logger.d { "Unsubscribing from channel $topic" }
        val websocket = realtime.websocket
        if(checkWebsocket(websocket, "Failed to unsubscribe from channel $topic")) return
        realtimeImpl.send(RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_LEAVE, buildJsonObject {}, websocket.makeRef(), joinRef.load()))
    }

    override suspend fun updateAuth(jwt: String?) {
        logger.d { "Updating auth token for channel $topic" }
        val websocket = realtime.websocket
        if(checkWebsocket(websocket, "Failed to update auth token for channel $topic")) return
        realtimeImpl.send(RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_ACCESS_TOKEN, buildJsonObject {
            put("access_token", jwt)
        }, websocket.makeRef(), joinRef.load()))
    }

    override suspend fun broadcast(event: String, payload: BroadcastPayload) {
        if(status.value != RealtimeChannel.Status.SUBSCRIBED) {
            logger.w { """
                Realtime broadcast() is automatically falling back to REST API.
                This behavior will be deprecated in the future.
                Please use httpSend() explicitly for REST delivery.""".trimIndent()
            }
            httpSend(event, payload)
        } else {
            val websocket = realtime.websocket
            if(checkWebsocket(websocket, "Failed to broadcast message on channel $topic")) return
            when(realtime.config.vsn) {
                RealtimeProtocolVersion.V1 -> when(payload) {
                    is BroadcastPayload.Binary -> error("Binary payloads are not supported in 1.0.0")
                    is BroadcastPayload.Json -> {
                        realtimeImpl.send(
                            RealtimeMessage(topic, "broadcast", buildJsonObject {
                                put("type", "broadcast")
                                put("event", event)
                                put("payload", payload.value)
                            }, websocket.makeRef(), joinRef.load())
                        )
                    }
                }
                RealtimeProtocolVersion.V2 -> {
                    val ref = websocket.makeRef()
                    realtime.send(RealtimeBroadcast(topic, event, payload).encodeBroadcast(joinRef.load(), ref))
                }
            }
        }
    }

    @SupabaseInternal
    override fun RealtimeChannel.addPostgresChange(data: PostgresJoinConfig) {
        clientChanges.add(data)
    }

    @SupabaseInternal
    override fun RealtimeChannel.removePostgresChange(data: PostgresJoinConfig) {
        clientChanges.remove(data)
    }

    override suspend fun track(state: JsonObject) {
        if(status.value != RealtimeChannel.Status.SUBSCRIBED) {
            error("You can only track your presence after subscribing to the channel. Did you forget to call `channel.subscribe()`?")
        }
        val payload = buildJsonObject {
            put("type", "presence")
            put("event", "track")
            putJsonObject("payload") {
                putJsonObject(state)
            }
        }
        val websocket = realtime.websocket
        if(checkWebsocket(websocket, "Failed to track presence on channel $topic")) return
        realtimeImpl.send(
            RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_PRESENCE, payload, websocket.makeRef(), joinRef.load())
        )
    }

    override suspend fun untrack() {
        val websocket = realtime.websocket
        if(checkWebsocket(websocket, "Failed to untrack presence on channel $topic")) return
        realtimeImpl.send(
            RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_PRESENCE, buildJsonObject {
                put("type", "presence")
                put("event", "untrack")
            }, websocket.makeRef(), joinRef.load())
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PostgresAction> RealtimeChannel.postgresChangeFlowInternal(
        action: KClass<T>,
        schema: String,
        filter: PostgresChangeFilter.() -> Unit
    ): Flow<T> {
        if(status.value == RealtimeChannel.Status.SUBSCRIBED) error("You cannot call postgresChangeFlow after joining the channel")
        val event = when(action) {
            PostgresAction.Insert::class -> "INSERT"
            PostgresAction.Update::class -> "UPDATE"
            PostgresAction.Delete::class -> "DELETE"
            PostgresAction.Select::class -> "SELECT"
            PostgresAction::class -> "*"
            else -> error("Unknown event type $action")
        }
        val postgrestBuilder = PostgresChangeFilter(event, schema).apply(filter)
        val config = postgrestBuilder.buildConfig()
        addPostgresChange(config)
        return callbackFlow {
            val callback: (PostgresAction) -> Unit = {
                if (action.isInstance(it)) {
                    trySend(it as T)
                }
            }

            val id = callbackManager.addPostgresCallback(config, callback)
            awaitClose {
                callbackManager.removeCallbackById(id)
                removePostgresChange(config)
            }
        }
    }

    override fun broadcastFlow(event: String): Flow<RealtimeBroadcast> = callbackFlow {
        val id = callbackManager.addBroadcastCallback(event) {
            trySend(it)
        }
        awaitClose { callbackManager.removeCallbackById(id) }
    }

    override fun systemFlow(): Flow<RealtimeSystemPayload> = callbackFlow {
        val id = callbackManager.addSystemCallback {
            trySend(it)
        }
        awaitClose { callbackManager.removeCallbackById(id) }
    }

    override fun errorFlow(): Flow<RealtimeChannelError> = callbackFlow {
        val id = callbackManager.addErrorCallback {
            trySend(it)
        }
        awaitClose { callbackManager.removeCallbackById(id) }
    }

  /*  override fun <T : Any> RealtimeChannel.broadcastFlowInternal(type: KType, event: String): Flow<T> = callbackFlow {
        val id = callbackManager.addBroadcastCallback(event) {
            val decodedValue = try {
                when(it) {
                    is RealtimeBroadcast.Binary if(type == typeOf<ByteArray>()) -> it.payload as T
                    is RealtimeBroadcast.Json -> supabaseClient.realtime.serializer.decode<T>(type, it.payload.toString())
                    else -> error("Received binary broadcast in event flow for $event, even though the specified type is not `ByteArray`")
                }
            } catch(e: Exception) {
                coroutineContext.ensureActive()
                logger.e(e) { "Couldn't decode $it as $type. The corresponding handler wasn't called" }
                null
            }
            decodedValue?.let { value -> trySend(value) }
        }
        awaitClose { callbackManager.removeCallbackById(id) }
    }*/

    override fun presenceChangeFlow(): Flow<PresenceAction> = callbackFlow {
        val callback: (PresenceAction) -> Unit = { action ->
            trySend(action)
        }
        val id = callbackManager.addPresenceCallback(callback)
        if(status.value == RealtimeChannel.Status.SUBSCRIBED && !presenceJoinConfig.enabled) {
            logger.d { "Resubscribing to channel $topic to enable presence..." }
            resubscribe()
        }
        awaitClose { callbackManager.removeCallbackById(id) }
    }

    override fun updateStatus(status: RealtimeChannel.Status) {
        when(status) {
            RealtimeChannel.Status.SUBSCRIBED -> {
                joinAttempt.store(0)
                cancelJoinTimeout()
            }
            RealtimeChannel.Status.UNSUBSCRIBING, RealtimeChannel.Status.UNSUBSCRIBED -> {
                cancelRejoin()
                cancelJoinTimeout()
            }
            RealtimeChannel.Status.SUBSCRIBING -> Unit
        }
        _status.value = status
    }

    private suspend fun resubscribe() {
        join(blockUntilSubscribed = false)
    }

    // Not the exception itself, so each blocking subscribe() throws one with its own stack trace
    private data class JoinEnded(val message: String, val error: RealtimeChannelError?)

    private companion object {
        // The Realtime server's default for postgres_changes_options.timeout
        val POSTGRES_CHANGES_WAIT_TIMEOUT = 15.seconds
        // The time the server takes to reject the join after waiting, like realtime-js's POSTGRES_CHANGES_WAIT_ERROR_GRACE
        val POSTGRES_CHANGES_WAIT_ERROR_GRACE = 10.seconds
    }

}

