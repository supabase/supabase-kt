package io.supabase.storage.vectors.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Vector object returned from queries with optional distance
 * @property key Unique identifier for the vector
 * @property data Vector embedding data (if requested)
 * @property metadata Arbitrary metadata (if requested)
 * @property distance Similarity distance from query vector (if requested)
 */
@Serializable
data class VectorMatch(
    val key: String,
    val data: VectorData? = null,
    val metadata: JsonObject? = null,
    val distance: Double? = null
)