package io.supabase.storage.vectors.index

import kotlinx.serialization.Serializable

/**
 * Metadata configuration for vector index
 * Defines which metadata keys should not be indexed for filtering
 * @property nonFilterableMetadataKeys Array of metadata keys that cannot be used in filters
 */
@Serializable
class MetadataConfiguration(
    val nonFilterableMetadataKeys: List<String>? = null
)