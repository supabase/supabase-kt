package io.supabase.storage.vectors.data

import kotlinx.serialization.Serializable

/**
 * Response from listing vectors
 * @property vectors Array of vector objects
 * @property nextToken Token for fetching next page (if more results exist)
 */
@Serializable
data class ListVectorsResponse(
    val vectors: List<VectorMatch>,
    val nextToken: String? = null
)
