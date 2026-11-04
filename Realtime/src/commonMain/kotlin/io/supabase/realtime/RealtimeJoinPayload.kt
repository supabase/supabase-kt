@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package io.supabase.realtime

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SupabaseInternal
@Serializable
data class RealtimeJoinPayload(
    val config: RealtimeJoinConfig
)

@SupabaseInternal
@Serializable
data class RealtimeJoinConfig(
    val broadcast: BroadcastJoinConfig,
    val presence: PresenceJoinConfig,
    @SerialName("postgres_changes")
    val postgresChanges: List<PostgresJoinConfig>,
    @SerialName("private")
    var isPrivate: Boolean
)

/**
 * @param acknowledgeBroadcasts Whether the server should send an acknowledgment message for each broadcast message
 * @param receiveOwnBroadcasts Whether you should receive your own broadcasts
 * @param replicationReady replication_ready option instructs the server to emit a `system` event once the
 * Postgres replication connection backing this channel is established and ready to
 * stream changes. Listen for it with `channel.on('system', {}, (payload) => ...)`; TODO: fix docs
 * the payload's `status` is `'ok'` (`message: 'Replication connection established'`)
 * on success or `'error'` if the connection is not ready in time.
 */
@Serializable
data class BroadcastJoinConfig(
    @SerialName("ack") var acknowledgeBroadcasts: Boolean,
    @SerialName("self") var receiveOwnBroadcasts: Boolean,
    @SerialName("replication_ready") var replicationReady: Boolean
)

/**
 * @param key Used to track presence payloads. Can be e.g. a user id
 * @param enabled Whether presence is enabled for this channel
 */
@Serializable
data class PresenceJoinConfig(
    var key: String,
    @EncodeDefault
    internal var enabled: Boolean = false
)

@SupabaseInternal
@Serializable
data class PostgresJoinConfig(val schema: String, val table: String? = null, val filter: String? = null, val event: String, val id: Int = 0) {

    override fun equals(other: Any?): Boolean {
        if(other !is PostgresJoinConfig) return false
        return other.schema == schema && other.table == table && other.filter == filter && (other.event == event || other.event == "*")
    }

    override fun hashCode(): Int {
        var result = schema.hashCode()
        result = 31 * result + (table?.hashCode() ?: 0)
        result = 31 * result + (filter?.hashCode() ?: 0)
        result = 31 * result + event.hashCode()
        return result
    }

}
