package io.supabase.realtime.broadcast

/**
 * Represents a broadcast used in [io.supabase.realtime.RealtimeChannel.broadcastFlow]
 * @param topic The channel's topic
 * @param event The broadcast event
 * @param payload The broadcast payload
 */
class RealtimeBroadcast(
    val topic: String,
    val event: String,
    val payload: BroadcastPayload
)