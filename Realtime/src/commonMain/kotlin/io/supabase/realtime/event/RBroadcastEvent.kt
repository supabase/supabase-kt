package io.supabase.realtime.event

import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.broadcast.BroadcastPayload
import io.supabase.realtime.broadcast.RealtimeBroadcast
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handles broadcast events
 */
data object RBroadcastEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        val event = message.payload["event"]?.jsonPrimitive?.content ?: ""
        channel.callbackManager.triggerBroadcast(RealtimeBroadcast(message.topic, event, BroadcastPayload.Json(message.payload["payload"]?.jsonObject ?: JsonObject(mutableMapOf()))))
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_BROADCAST
    }

}