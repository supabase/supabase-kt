package io.github.jan.supabase.realtime.event

import io.github.jan.supabase.decodeIfNotEmptyOrDefault
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeMessage

/**
 * Event that handles the presence state event
 */
data object RPresenceStateEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        val joins = message.payload.decodeIfNotEmptyOrDefault(mapOf<String, Presence>())
        val leaves = channel.callbackManager.presenceState().filterKeys { it !in joins }
        channel.callbackManager.triggerPresenceDiff(joins, leaves)
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_PRESENCE_STATE
    }

}