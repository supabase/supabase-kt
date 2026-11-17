package io.supabase.realtime

import io.supabase.annotations.SupabaseInternal
import io.supabase.realtime.broadcast.RealtimeBroadcast

@SupabaseInternal
sealed interface RealtimeCallback <T> {

    val callback: (T) -> Unit
    val id: Int

    class PostgresCallback(
        override val callback: (PostgresAction) -> Unit,
        val filter: PostgresJoinConfig,
        override val id: Int
    ): RealtimeCallback<PostgresAction>

    class BroadcastCallback(
        override val callback: (RealtimeBroadcast) -> Unit,
        val event: String,
        override val id: Int
    ): RealtimeCallback<RealtimeBroadcast>

    class PresenceCallback(
        override val callback: (PresenceAction) -> Unit,
        override val id: Int
    ): RealtimeCallback<PresenceAction>

    class SystemCallback(
        override val callback: (RealtimeSystemPayload) -> Unit,
        override val id: Int
    ): RealtimeCallback<RealtimeSystemPayload>

}