package io.supabase.storage.vectors.index

import io.supabase.serializer.UnixTimestampSerializer
import io.supabase.storage.vectors.DistanceMetric
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Vector index configuration and metadata
 * @property indexName Unique name of the index within the bucket
 * @property vectorBucketName Name of the parent vector bucket
 * @property dataType Data type of vector components (currently only 'float32')
 * @property dimension Dimensionality of vectors (e.g., 384, 768, 1536)
 * @property distanceMetric Similarity metric used for queries
 * @property metadataConfiguration Configuration for metadata filtering
 * @property creationTime Unix timestamp of when the index was created
 */
@Serializable
data class VectorIndex(
    val indexName: String,
    val vectorBucketName: String,
    val dataType: VectorDataType,
    val dimension: Int,
    val distanceMetric: DistanceMetric,
    val metadataConfiguration: MetadataConfiguration? = null,
    @Serializable(with = UnixTimestampSerializer::class) private val creationTime: Instant? = null
)
