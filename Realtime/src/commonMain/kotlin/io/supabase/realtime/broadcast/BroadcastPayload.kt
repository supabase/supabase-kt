package io.supabase.realtime.broadcast

import io.supabase.realtime.RealtimeChannel
import kotlinx.serialization.json.JsonElement

/**
 * Represents the payload used in [RealtimeChannel.httpSend] and [RealtimeChannel.broadcast]
 */
sealed interface BroadcastPayload {

    /**
     * JSON-encoded payload via [value]
     */
    data class Json(val value: JsonElement): BroadcastPayload

    /**
     * Binary payload via [data]
     */
    class Binary(val data: ByteArray): BroadcastPayload

}