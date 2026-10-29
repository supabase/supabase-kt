package io.github.jan.supabase.realtime

/**
 * Thrown by [RealtimeChannel.subscribe] with `blockUntilSubscribed = true` when the channel stops trying to join before
 * it's subscribed: it gave up after [Realtime.Config.maxAttempts] or was closed.
 *
 * @param error The error the channel gave up after, the same one [RealtimeChannel.errorFlow] emitted. Null if the channel was closed.
 */
class RealtimeChannelJoinException(message: String, val error: RealtimeChannelError?) : Exception(message)
