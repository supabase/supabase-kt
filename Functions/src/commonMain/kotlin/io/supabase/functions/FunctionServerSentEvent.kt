package io.supabase.functions

import io.supabase.SupabaseSerializer
import io.supabase.decode

/**
 * Represents a Server-Sent Event received from an edge function.
 *
 * This is a wrapper around Ktor's [io.ktor.sse.ServerSentEvent] that adds [decodeAs] for
 * conveniently deserializing the [data] payload using the Functions plugin's serializer.
 *
 * @property data The event data payload, if present
 * @property event The event type, if present
 * @property id The event ID, if present
 */
class FunctionServerSentEvent(
    val data: String?,
    val event: String?,
    val id: String?,
    @PublishedApi internal val serializer: SupabaseSerializer
) {

    /**
     * Decodes the [data] payload as the specified type [T] using the Functions plugin's serializer.
     * @throws IllegalStateException if [data] is null
     */
    inline fun <reified T> decodeAs(): T {
        val raw = data ?: error("Cannot decode SSE event: data is null")
        return serializer.decode(raw)
    }

}
