package io.github.jan.supabase.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.AuthDependentPluginConfig
import io.github.jan.supabase.auth.resolveAccessToken
import io.github.jan.supabase.plugins.CustomSerializationConfig
import io.github.jan.supabase.plugins.CustomSerializationPlugin
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocket
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocketFactory
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.supabaseJson
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

/**
 * Plugin for interacting with the supabase realtime api
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Realtime)
 * }
 * ```
 *
 * You can then create a channel:
 * ```kotlin
 * val channel = supabase.realtime.channel("channelId")
 * ```
 * Then listen to events on the channel:
 * ```kotlin
 * val productChangeFlow = channel.postgrestChangeFlow<PostgrestAction.Insert>(schema = "public") {
 *    table = "products"
 * }.map { it.decodeRecord<Product>() }
 * ```
 * And at last you have to subscribe to the channel:
 * ```kotlin
 * channel.subscribe()
 * ```
 */
interface Realtime : MainPlugin<Realtime.Config>, CustomSerializationPlugin {

    /**
     * The current status of the realtime connection
     */
    val status: StateFlow<Status>

    /**
     * A map of all active the subscriptions
     */
    val subscriptions: Map<String, RealtimeChannel>

    @SupabaseInternal
    val websocket: RealtimeWebsocket?

    /**
     * Connects to the realtime websocket. The url will be taken from the custom provided [Realtime.Config.customUrl] or [SupabaseClient]
     */
    suspend fun connect()

    /**
     * Disconnects from the realtime websocket
     */
    fun disconnect()

    /**
     * Unsubscribes and removes a channel from the [subscriptions]
     * @param channel The channel to remove
     */
    suspend fun removeChannel(channel: RealtimeChannel)

    /**
     * Adds a channel to the [subscriptions] without subscribing to it.
     */
    @SupabaseInternal
    fun addChannel(channel: RealtimeChannel)

    /**
     * Unsubscribes and removes all channels from the [subscriptions]
     */
    suspend fun removeAllChannels()

    /**
     * Blocks your current coroutine until the websocket connection is closed
     */
    suspend fun block()

    /**
     * Sends a message to the realtime websocket
     * @param message The message to send
     */
    @SupabaseInternal
    suspend fun send(message: RealtimeMessage)

    /**
     * Sends a binary payload to the realtime websocket
     * @param data The data to send
     */
    @SupabaseInternal
    suspend fun send(data: ByteArray)

    /**
     * Sets the JWT access token used for channel subscription authorization and Realtime RLS.
     *
     * If [token] is null, the token will be resolved using the [Realtime.Config.accessToken] provider.
     *
     * @param token The JWT access token
     */
    suspend fun setAuth(token: String? = null)

    /**
     * Creates a new [RealtimeChannel] and adds it to the [subscriptions]
     *
     * - This method does not subscribe to the channel. You have to call [RealtimeChannel.subscribe] to do so.
     * - If a channel with the same [channelId] already exists, it will be returned
     *
     * @param channelId The id of the channel
     * @param builder The builder for the channel
     */
    fun channel(channelId: String, builder: RealtimeChannelBuilder): RealtimeChannel

    /**
     * @property websocketConfig Custom configuration for the Ktor Websocket Client. This only applies if [Realtime.Config.websocketFactory] is null.
     * @property secure Whether to use wss or ws. Defaults to [SupabaseClient.useHTTPS] when null
     * @property disconnectOnSessionLoss Whether to disconnect from the websocket when the session is lost. Defaults to true
     * @property reconnectDelay The delay between reconnect attempts. Defaults to 7 seconds
     * @property heartbeatInterval The interval between heartbeat messages. Defaults to 15 seconds
     * @property connectOnSubscribe Whether to connect to the websocket when subscribing to a channel. Defaults to true
     * @property disconnectOnNoSubscriptions Whether to disconnect from the websocket when there are no more subscriptions. Defaults to true
     * @property serializer A serializer used for serializing/deserializing objects e.g. in [PresenceAction.decodeJoinsAs] or [RealtimeChannel.broadcast]. Defaults to [KotlinXSerializer]
     * @property websocketFactory A custom websocket factory. If this is set, the [websocketConfig] will be ignored
     * @property rejoinDelay The interval between channel rejoin attempts
     * @property maxAttempts The maximum number of times a channel will try to rejoin after an error before giving up. Defaults to 5
     * @property timeout How long a channel waits for the server to answer its join before trying again. A channel that waits for its postgres changes subscription ([PostgresChangesOptions.wait]) waits at least 10 seconds longer than the server does. Defaults to 10 seconds
     * @property disconnectOnEmptyChannelsAfter Delay before disconnecting from the realtime socket after the last channel was removed. If null, it defaults to `2*heartbeatInterval`
     * @property vsn The realtime protocol version. [RealtimeProtocolVersion.V2] supports binary payloads and is more efficient.
     */
    @Suppress("MagicNumber")
    class Config: MainConfig(), CustomSerializationConfig, AuthDependentPluginConfig {

        var websocketConfig: WebSockets.Config.() -> Unit = {}
        var secure: Boolean? = null
        var heartbeatInterval: Duration = 15.seconds
        var reconnectDelay: Duration = 7.seconds
        var rejoinDelay: Duration = 2.seconds
        var disconnectOnEmptyChannelsAfter: Duration? = null
        var maxAttempts: Int = 5
        var timeout: Duration = 10.seconds
        var disconnectOnSessionLoss: Boolean = true
        var connectOnSubscribe: Boolean = true
        @SupabaseInternal var websocketFactory: RealtimeWebsocketFactory? = null
        var disconnectOnNoSubscriptions: Boolean = true
        var vsn = RealtimeProtocolVersion.V2
        override var requireValidSession: Boolean = true

        internal var customAccessTokenProvider = false
        internal var coroutineScope: CoroutineScope? = null

        internal val disconnectDelay by lazy {
            disconnectOnEmptyChannelsAfter ?: (2 * heartbeatInterval)
        }

        /**
         * A custom access token provider. If this is set, the [SupabaseClient] will not be used to resolve the access token.
         */
        var accessToken: suspend SupabaseClient.() -> String? = { resolveAccessToken(realtime, keyAsFallback = false) }
            set(value) {
                field = value
                customAccessTokenProvider = true
            }
        override var serializer: SupabaseSerializer? = null

    }

    companion object : SupabasePluginProvider<Config, Realtime> {

        override val key = "realtime"

        /**
         * The tag for the Realtime logger.
         */
        const val LOGGING_TAG = "Supabase-Realtime"

        /**
         * The current realtime api version
         */
        const val API_VERSION = 1

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)
        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            if(config.websocketFactory == null) {
                builder.httpConfig {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(supabaseJson)
                        config.websocketConfig(this)
                    }
                }
            }
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): Realtime = RealtimeImpl(supabaseClient, config)

    }

    /**
     * The current status of the realtime connection
     */
    enum class Status {
        /**
         * [Realtime] is disconnected from Supabase Realtime
         */
        DISCONNECTED,

        /**
         * [Realtime] is connecting to Supabase Realtime
         */
        CONNECTING,

        /**
         * [Realtime] is connected to Supabase Realtime
         */
        CONNECTED,
    }

}

/**
 * Creates a new [RealtimeChannel] and adds it to the [Realtime.subscriptions]
 *
 * - This method does not subscribe to the channel. You have to call [RealtimeChannel.subscribe] to do so.
 * - If a channel with the same [channelId] already exists, it will be returned
 *
 * @param channelId The id of the channel
 * @param builder The builder for the channel
 */
inline fun Realtime.channel(channelId: String, builder: RealtimeChannelBuilder.() -> Unit = {}): RealtimeChannel = channel(channelId, RealtimeChannelBuilder(RealtimeTopic.withChannelId(channelId)).apply(builder))


/**
 * Creates a new [RealtimeChannel]
 */
inline fun SupabaseClient.channel(channelId: String, builder: RealtimeChannelBuilder.() -> Unit = {}): RealtimeChannel = realtime.channel(channelId, builder)

/**
 * Supabase Realtime is a way to listen to changes in the PostgreSQL database via websockets
 */
val SupabaseClient.realtime: Realtime
    get() = pluginManager.getPlugin(Realtime)