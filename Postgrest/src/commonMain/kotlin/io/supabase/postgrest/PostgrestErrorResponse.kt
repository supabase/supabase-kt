package io.supabase.postgrest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class PostgrestErrorResponse(
    val message: String,
    val hint: String? = null,
    val details: JsonElement? = null,
    val code: String? = null,
)
