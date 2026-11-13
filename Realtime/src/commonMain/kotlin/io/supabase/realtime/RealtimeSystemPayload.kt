package io.supabase.realtime

import kotlinx.serialization.Serializable

/**
 * Payload of a `system` event emitted by the server.
 *
 * Most notably, when a channel is created with [BroadcastJoinConfig.replicationReady]: `true`,
 * the server sends one of these once the Postgres replication connection is ready
 * (`status: 'ok'`) or fails to become ready in time (`status: 'error'`).
 *
 * @param extension The extension that produced the message, e.g. `'system'` or `'postgres_changes'`.
 * @param status `'ok'` on success, `'error'` on failure.
 * @param message Human-readable description, e.g. `'Replication connection established'`.
 * @param channel The channel (sub)topic the message refers to.
 */
@Serializable
data class RealtimeSystemPayload(
    val extension: String,
    val status: String,
    val message: String,
    val channel: String
)