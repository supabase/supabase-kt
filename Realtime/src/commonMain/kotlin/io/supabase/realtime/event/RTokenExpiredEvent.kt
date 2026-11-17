package io.supabase.realtime.event

import io.supabase.logging.w
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event that handles the token expired event
 */
data object RTokenExpiredEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        channel.logger.w { "Received token expired event. This should not happen, please report this warning." }
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_SYSTEM && message.payload["message"]?.jsonPrimitive?.content?.contains("access token has expired") ?: false
    }

}