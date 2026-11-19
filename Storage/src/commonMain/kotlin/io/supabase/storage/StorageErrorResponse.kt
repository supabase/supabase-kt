package io.supabase.storage

import kotlinx.serialization.Serializable

@Serializable
internal data class StorageErrorResponse(
    val statusCode: Int,
    val error: String,
    val message: String,
    val code: String? = null
)
