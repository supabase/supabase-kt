package io.github.jan.supabase.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.realtime.broadcast.BroadcastPayload
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

/**
 * Represents a realtime channel
 */
interface RealtimeChannel {

    /**
     * The status of the channel as a [StateFlow]
     */
    val status: StateFlow<Status>

    /**
     * The topic of the channel
     */
    val topic: String

    /**
     * The current [SupabaseClient]
     */
    val supabaseClient: SupabaseClient

    /**
     * The realtime instance
     */
    val realtime: Realtime

    @SupabaseInternal
    val callbackManager: CallbackManager

    @SupabaseInternal
    val logger: SupabaseLogger

    /**
     * Subscribes to the channel
     * @param blockUntilSubscribed if true, the method will block the coroutine until the [status] is [Status.SUBSCRIBED].
     * It keeps waiting while the connection is lost, since the channel rejoins when the client reconnects.
     * @throws RealtimeChannelJoinException if [blockUntilSubscribed] is true and the channel stops trying to join before it's
     * subscribed: it gave up after [Realtime.Config.maxAttempts] or was closed
     */
    suspend fun subscribe(blockUntilSubscribed: Boolean = false)

    /**
     * Updates the JWT token for this channel
     */
    suspend fun updateAuth(jwt: String?)

    /**
     * Unsubscribes from the channel
     */
    suspend fun unsubscribe()

    /**
     * Sends a message to everyone who joined the channel. Can be used even if you aren't connected to the channel.
     * @param event the broadcast event. Example: mouse_cursor
     * @param payload the broadcast payload
     */
    suspend fun broadcast(event: String, payload: BroadcastPayload)

    /**
     * Sends a broadcast message explicitly via REST API.
     *
     * This method always uses the REST API endpoint regardless of WebSocket connection state.
     * Useful when you want to guarantee REST delivery or when gradually migrating from implicit REST fallback.
     *
     * Payloads that are a [ByteArray] are sent as
     * `application/octet-stream`; all other payloads are JSON-encoded.
     * @param event The name of the broadcast event
     * @param payload Payload to be sent (required)
     * @param builder Options including timeout
     * @throws RestException If any error appears after sending the payload
     * @throws IllegalStateException If the realtime version does not support sending broadcasts via HTTP
     *
     */
    suspend fun httpSend(event: String, payload: BroadcastPayload, builder: HttpSendBuilder.() -> Unit = {})

    /**
     * Store an object in your presence's state. Other clients can get this data when you either join or leave the channel.
     * Use this method again to update the state.
     * @param state the data to store
     */
    suspend fun track(state: JsonObject)

    /**
     * Removes the object from your presence's state
     */
    suspend fun untrack()

    /**
     * Non-inline variant of [postgresChangeFlow] for implementation and mocking purposes
     */
    @SupabaseInternal
    fun <T : PostgresAction> RealtimeChannel.postgresChangeFlowInternal(action: KClass<T>, schema: String, filter: PostgresChangeFilter.() -> Unit = {}): Flow<T>

    /**
     * Broadcasts can be messages sent by other clients within the same channel under a specific [event].
     *
     * Example:
     * ```kotlin
     * val messageFlow = channel.broadcastFlow("message")
     * messageFlow.collect {
     *    when(it) {
     *      is BroadcastPayload.Json -> println("Received message: ${it.value}")
     *      is BroadcastPayload.Binary -> println("Received binary payload")
     *    }
     * }
     * ```
     *
     * @param event When a message is sent by another client, it will be sent under a specific event. This is the event that you want to listen to
     */
    fun broadcastFlow(event: String): Flow<RealtimeBroadcast>

    /**
     * Listen for clients joining / leaving the channel using presences
     *
     * Example:
     * ```kotlin
     * val presenceChangeFlow = channel.presenceChangeFlow()
     *
     * presenceChangeFlow.collect {
     *    val joins = it.decodeJoinsAs<User>()
     *    val leaves = it.decodeLeavesAs<User>()
     * }
     * ```
     */
    fun presenceChangeFlow(): Flow<PresenceAction>

    /**
     * Listen for `system` events on this channel.
     *
     * Opt in to the replication-ready notification with [BroadcastJoinConfig.replicationReady]: `true` when creating the channel, then
     * watch for [RealtimeSystemPayload.status] == `"ok"` to know the Postgres replication connection is ready.
     *
     * Errors arrive with [RealtimeSystemPayload.status] == `"error"`, e.g. when the server refuses a postgres_changes
     * subscription. 
     */
    fun systemFlow(): Flow<RealtimeSystemPayload>

    /**
     * Listen for errors on this channel: a rejected join, a channel error, or a join the server didn't answer within
     * [Realtime.Config.timeout]. Like realtime-js's `CHANNEL_ERROR` and `TIMED_OUT`. The channel rejoins after each one,
     * until [Realtime.Config.maxAttempts] is reached.
     *
     * A refused postgres changes subscription arrives here as a rejected join when the channel waits for it
     * ([PostgresChangesOptions.wait]), and on [systemFlow] otherwise.
     */
    fun errorFlow(): Flow<RealtimeChannelError>

    @SupabaseInternal
    fun RealtimeChannel.addPostgresChange(data: PostgresJoinConfig)

    @SupabaseInternal
    fun RealtimeChannel.removePostgresChange(data: PostgresJoinConfig)

    @SupabaseInternal
    fun updateStatus(status: Status)

    @SupabaseInternal
    fun scheduleRejoin()

    @SupabaseInternal
    fun teardown()

    /**
     * Represents the status of a channel
     */
    enum class Status {
        /**
         * The [RealtimeChannel] is currently unsubscribed
         */
        UNSUBSCRIBED,

        /**
         * The [RealtimeChannel] is currently in the process of subscribing
         */
        SUBSCRIBING,

        /**
         * The [RealtimeChannel] is subscribed
         */
        SUBSCRIBED,

        /**
         * The [RealtimeChannel] is in the process of unsubscribing
         */
        UNSUBSCRIBING,
    }

    /**
     * @see RealtimeChannel
     */
    @Suppress("UndocumentedPublicProperty")
    companion object {
        const val CHANNEL_EVENT_JOIN = "phx_join"
        const val CHANNEL_EVENT_LEAVE = "phx_leave"
        const val CHANNEL_EVENT_CLOSE = "phx_close"
        const val CHANNEL_EVENT_ERROR = "phx_error"
        const val CHANNEL_EVENT_REPLY = "phx_reply"
        const val CHANNEL_EVENT_SYSTEM = "system"
        const val CHANNEL_EVENT_BROADCAST = "broadcast"
        const val CHANNEL_EVENT_ACCESS_TOKEN = "access_token"
        const val CHANNEL_EVENT_PRESENCE = "presence"
        const val CHANNEL_EVENT_PRESENCE_DIFF = "presence_diff"
        const val CHANNEL_EVENT_PRESENCE_STATE = "presence_state"
        const val CHANNEL_EVENT_POSTGRES_CHANGES = "postgres_changes"
    }

}