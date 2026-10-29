package io.github.jan.supabase.realtime

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.realtime.annotations.ChannelDsl

/**
 * Used to build a realtime channel
 */
@ChannelDsl
class RealtimeChannelBuilder @PublishedApi internal constructor(
    private val topic: String,
) {

    private var broadcastJoinConfig = BroadcastJoinConfig(acknowledgeBroadcasts = false, receiveOwnBroadcasts = false, replicationReady = false)
    private var presenceJoinConfig = PresenceJoinConfig("", false)
    private var postgresChangesOptions: PostgresChangesOptions? = null

    /**
     * Whether this channel should be private.
     */
    var isPrivate = false

    /**
     * Sets the broadcast join config
     */
    fun broadcast(block: BroadcastJoinConfig.() -> Unit) {
        broadcastJoinConfig = BroadcastJoinConfig(acknowledgeBroadcasts = false, receiveOwnBroadcasts = false, replicationReady = false).apply(block)
    }

    /**
     * Sets the presence join config
     * @param block The presence join config
     */
    fun presence(block: PresenceJoinConfig.() -> Unit) {
        presenceJoinConfig = PresenceJoinConfig("", false).apply(block)
    }

    /**
     * Sets the postgres changes options
     * @param block The postgres changes options
     */
    fun postgresChangesOptions(block: PostgresChangesOptions.() -> Unit) {
        postgresChangesOptions = PostgresChangesOptions().apply(block)
    }

    @SupabaseInternal
    fun build(realtime: Realtime): RealtimeChannel {
        return RealtimeChannelImpl(
            realtime,
            topic,
            broadcastJoinConfig,
            presenceJoinConfig,
            isPrivate,
            postgresChangesOptions
        )
    }

}