package io.supabase.realtime.event

import io.supabase.logging.d
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event that handles the system reply event
 */
data object RSystemReplyEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        channel.logger.d { "Received system reply: ${message.payload}." }
        if(channel.status.value == RealtimeChannel.Status.UNSUBSCRIBING) {
            channel.updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
            channel.logger.d { "Unsubscribed from channel ${message.topic}" }
        }
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_REPLY && message.payload["status"]?.jsonPrimitive?.content == "ok" &&
            !RPostgresServerChangesEvent.appliesTo(message)
    }

}