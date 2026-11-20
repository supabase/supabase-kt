package io.supabase.realtime.event

import io.supabase.logging.e
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeChannelError
import io.supabase.realtime.RealtimeChannelImpl
import io.supabase.realtime.RealtimeMessage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
        channel.logger.e { "Received an error in channel ${message.topic}. That could be as a result of an invalid access token.\n${message.payload}" }
        channel.rejoinAfter(channelError(message))
    }

    private fun channelError(message: RealtimeMessage): RealtimeChannelError {
        val rejectedJoin = message.event == RealtimeChannel.CHANNEL_EVENT_REPLY
        // A rejected join has its reason in the reply's response
        val details = if(rejectedJoin) message.payload["response"] as? JsonObject else message.payload
        return RealtimeChannelError(
            type = if(rejectedJoin) RealtimeChannelError.Type.JOIN_REJECTED else RealtimeChannelError.Type.CHANNEL_ERROR,
            reason = (details?.get("reason") as? JsonPrimitive)?.contentOrNull,
            payload = message.payload
        )
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_ERROR ||
            message.event == RealtimeChannel.CHANNEL_EVENT_REPLY && message.payload["status"]?.jsonPrimitive?.content == "error"
    }

}