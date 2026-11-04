package io.supabase.realtime.event

import io.supabase.logging.d
import io.supabase.logging.e
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.RealtimeSystemPayload
import io.supabase.supabaseJson
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event that handles the system event
 */
data object RSystemEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        if(message.payload["status"]?.jsonPrimitive?.content == "ok") {
            channel.logger.d { "Subscribed to channel ${message.topic}" }
            channel.updateStatus(RealtimeChannel.Status.SUBSCRIBED)
        } else {
            // e.g. a refused postgres_changes subscription. Like realtime-js, the channel stays joined.
            channel.logger.e { "Received an error in channel ${message.topic}: ${message.payload}" }
        }
        val payload = supabaseJson.decodeFromJsonElement<RealtimeSystemPayload>(message.payload)
        channel.callbackManager.triggerSystem(payload)
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_SYSTEM && message.payload["status"]?.jsonPrimitive?.content in setOf("ok", "error") &&
            !RTokenExpiredEvent.appliesTo(message)
    }

}
