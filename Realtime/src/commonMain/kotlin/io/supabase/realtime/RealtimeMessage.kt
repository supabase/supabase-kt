package io.supabase.realtime

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents a message retrieved by the [RealtimeChannel]
 */
@Serializable
@SupabaseInternal
data class RealtimeMessage(val topic: String, val event: String, val payload: JsonObject, val ref: String?, @SerialName("join_ref") val joinRef: String? = null)