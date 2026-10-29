@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package io.github.jan.supabase.realtime

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    var isPrivate: Boolean,
    @SerialName("postgres_changes_options")
    val postgresChangesOptions: PostgresChangesOptions? = null
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
 * By default a channel is [RealtimeChannel.Status.SUBSCRIBED] as soon as it has joined, which can be before the server has
 * set up its postgres changes subscription, or refused it (see [RealtimeChannel.systemFlow]).
 *
 * @param wait Whether the server holds the join until the postgres changes subscription is set up, so that
 * [RealtimeChannel.Status.SUBSCRIBED] means changes are being received. If it can't be set up, the server rejects the join
 * (e.g. `RealtimeDisabledForConfiguration` for a table that isn't enabled for Realtime), the reason reaches
 * [RealtimeChannel.errorFlow], and the channel retries it like any other channel error. The channel's join timeout is
 * extended to outlast the server's wait (see [Realtime.Config.timeout]). Has no effect on a channel without postgres
 * changes. Realtime servers without support for it ignore it.
 * @param timeout How long the server holds the join. Null uses the server's default (15 s); the server caps it at its own maximum.
 */
@Serializable
data class PostgresChangesOptions(
    var wait: Boolean = false,
    @Serializable(with = MillisecondsSerializer::class)
    var timeout: Duration? = null
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

private object MillisecondsSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("MillisecondsDuration", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeMilliseconds)
    override fun deserialize(decoder: Decoder): Duration = decoder.decodeLong().milliseconds
}
