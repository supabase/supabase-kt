package io.github.jan.supabase.realtime

import kotlinx.serialization.json.JsonObject

/**
 * An error for a channel's join or for the joined channel, like realtime-js's `CHANNEL_ERROR` and `TIMED_OUT`. The channel
 * rejoins after each one until [Realtime.Config.maxAttempts] is reached.
 *
 * @param type What went wrong
 * @param reason The server's reason, e.g. `RealtimeDisabledForConfiguration: ...` for a rejected join. A channel error usually has none.
 * @param payload The payload of the server's message. Null for [Type.TIMED_OUT], which has none.
 */
data class RealtimeChannelError(
    val type: Type,
    val reason: String?,
    val payload: JsonObject?
) {

    /**
     * The kind of [RealtimeChannelError]
     */
    enum class Type {
        /**
         * The server rejected the channel's join, e.g. a postgres changes subscription it couldn't set up when the channel
         * waits for it (see [PostgresChangesOptions.wait])
         */
        JOIN_REJECTED,

        /**
         * The server reported an error (`phx_error`) for the channel, e.g. when its process on the server crashed
         */
        CHANNEL_ERROR,

        /**
         * The server didn't answer the channel's join within [Realtime.Config.timeout]
         */
        TIMED_OUT
    }

}
