package io.github.jan.supabase.realtime.event

import io.github.jan.supabase.logging.d
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannelImpl
import io.github.jan.supabase.realtime.RealtimeMessage

/**
 * Event that handles the closing of a channel
 */
data object RCloseEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        channel.realtime.removeChannel(channel)
        channel.logger.d { "Unsubscribed from channel ${message.topic}" }
        (channel as RealtimeChannelImpl).endJoin("Channel ${message.topic} was closed", null)
        channel.updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_CLOSE
    }

}