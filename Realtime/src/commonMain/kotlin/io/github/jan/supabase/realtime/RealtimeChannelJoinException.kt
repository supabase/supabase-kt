package io.github.jan.supabase.realtime

/**
 * Thrown by [RealtimeChannel.subscribe] with `blockUntilSubscribed = true` when the channel stops trying to join before
 * it's subscribed: it gave up after [Realtime.Config.maxAttempts], was closed, unsubscribed or removed, or the client disconnected.
 *
 * @param error The error the channel gave up after, the same one [RealtimeChannel.errorFlow] emitted. Null if it didn't give up.
 */
class RealtimeChannelJoinException(message: String, val error: RealtimeChannelError?) : Exception(message)
