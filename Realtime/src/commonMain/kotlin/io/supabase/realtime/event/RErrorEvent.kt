package io.supabase.realtime.event

import io.supabase.logging.e
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeChannelImpl
import io.supabase.realtime.RealtimeMessage
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Event that handles an error event
 */
data object RErrorEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        channel as RealtimeChannelImpl
        // Only rejoin if the join itself was rejected
        if(message.event == RealtimeChannel.CHANNEL_EVENT_REPLY && channel.status.value != RealtimeChannel.Status.SUBSCRIBING) {
            channel.logger.e { "Push to channel ${message.topic} failed: ${message.payload}" }
            return
        }
        val currentAttempt = channel.joinAttempt.incrementAndFetch()
        if(currentAttempt >= channel.realtime.config.maxAttempts) {
            channel.logger.e { "Failed to rejoin channel ${message.topic} after $currentAttempt attempts. Giving up. ${message.payload}" }
            channel.updateStatus(RealtimeChannel.Status.UNSUBSCRIBED)
            return
        }
        channel.logger.e { "Received an error in channel ${message.topic}. That could be as a result of an invalid access token. Trying to rejoin the channel...\n${message.payload}" }
        channel.scheduleRejoin()
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_ERROR ||
            message.event == RealtimeChannel.CHANNEL_EVENT_REPLY && message.payload["status"]?.jsonPrimitive?.content == "error"
    }

}