package io.supabase.realtime

import io.supabase.annotations.SupabaseInternal
import io.supabase.encodeToJsonElement
import io.supabase.exceptions.RestException
import io.supabase.logging.e
import io.supabase.logging.i
import io.supabase.realtime.broadcast.BroadcastPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.typeOf

/**
 * Listen for postgres changes in a channel.
 *
 * Example:
 * ```kotlin
 * val productChangeFlow = channel.postgrestChangeFlow<PostgresAction.Update>("public") {
 *    table = "products"
 * }.map { it.decodeRecord<Product>() }
 * ```
 *
 * If you just want to check for changes and also retrieve initial values, you can use the [postgresListDataFlow] or [postgresSingleDataFlow] functions.
 *
 * @param T The event type you want to listen to (e.g. [PostgresAction.Update] for updates or only [PostgresAction] for all)
 * @param schema The schema name of the table that is being monitored. For normal supabase tables that might be "public".
 */
inline fun <reified T : PostgresAction> RealtimeChannel.postgresChangeFlow(
    schema: String,
    noinline filter: PostgresChangeFilter.() -> Unit = {}
): Flow<T> = postgresChangeFlowInternal(T::class, schema, filter)

/**
 * Broadcasts can be messages sent by other clients within the same channel under a specific [event].
 *
 * Example:
 * ```kotlin
 * val messageFlow = channel.broadcastFlow<Message>("message")
 * messageFlow.collect {
 *    println("Received message: $it")
 * }
 * ```
 *
 * @param event When a message is sent by another client, it will be sent under a specific event. This is the event that you want to listen to
 */
@OptIn(SupabaseInternal::class)
inline fun <reified T : Any> RealtimeChannel.broadcastFlow(event: String): Flow<T> = broadcastFlow(event).mapNotNull {
    when (val payload = it.payload) {
        is BroadcastPayload.Binary if (T::class == ByteArray::class) -> it.payload.data as T
        is BroadcastPayload.Json -> try {
            realtime.serializer.decode(typeOf<T>(), payload.value.toString())
        } catch (e: Exception) {
            logger.e(e) { "Dropped json broadcast, because the payload could not be decoded as ${T::class}" }
            null
        }

        else -> {
            logger.i { "Dropped binary broadcast, because the expected type is ${T::class}" }
            null
        }
    }
}


/**
 * Sends a message to everyone who joined the channel. Can be used even if you aren't connected to the channel.
 * @param event the broadcast event. Example: mouse_cursor
 * @param message the message to send as [T] (can only be something that can be encoded as a JSON object)
 */
suspend inline fun <reified T : Any> RealtimeChannel.broadcast(event: String, message: T) =
    broadcast(event, BroadcastPayload.Json(supabaseClient.realtime.serializer.encodeToJsonElement(message).jsonObject))

/**
 * Sends a message to everyone who joined the channel. Can be used even if you aren't connected to the channel. Requires [Realtime.Config.vsn] to be set to [RealtimeProtocolVersion.V2]
 * @param event the broadcast event. Example: mouse_cursor
 * @param data the binary data to send
 */
suspend inline fun <reified T : Any> RealtimeChannel.broadcast(event: String, data: ByteArray) =
    broadcast(event, BroadcastPayload.Binary(data))

/**
 * Store an object in your presence's state. Other clients can get this data when you either join or leave the channel.
 * Use this method again to update the state.
 *
 * Example:
 * ```kotlin
 * @Serializable
 * data class PresenceData(val name: String)
 *
 * channel.track(PresenceData("Your Name"))
 * ```
 *
 * @param state the data to store (can only be something that can be encoded as a json object)
 */
suspend inline fun <reified T : Any> RealtimeChannel.track(state: T) = track(supabaseClient.realtime.serializer.encodeToJsonElement(state).jsonObject)

/**
 * Sends a broadcast message explicitly via REST API.
 *
 * This method always uses the REST API endpoint regardless of WebSocket connection state.
 * Useful when you want to guarantee REST delivery or when gradually migrating from implicit REST fallback.
 *
 * @param event The name of the broadcast event
 * @param payload Payload to be sent (required), encoded via [Realtime.serializer]
 * @param builder Options including timeout
 * @throws RestException If any error appears after sending the payload
 * @throws IllegalStateException If the realtime version does not support sending broadcasts via HTTP
 *
 */
suspend inline fun <reified T> RealtimeChannel.httpSend(event: String, payload: T, noinline builder: HttpSendBuilder.() -> Unit = {}) =
    httpSend(event, BroadcastPayload.Json(realtime.serializer.encodeToJsonElement(payload)), builder)