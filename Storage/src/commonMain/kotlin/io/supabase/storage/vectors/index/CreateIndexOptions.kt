package io.supabase.storage.vectors.index

import io.supabase.dsl.required
import io.supabase.storage.vectors.DistanceMetric
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Options for creating a vector index.
 *
 * @property vectorBucketName Name of the vector bucket where the index will be created
 * @property indexName Name of the index to create
 * @property dataType Data type of vectors stored in the index
 * @property dimension Number of dimensions in indexed vectors
 * @property distanceMetric Distance metric used for similarity search
 * @property metadataConfiguration Optional metadata indexing configuration
 */
class CreateIndexOptions(val vectorBucketName: String) {

    /** Name of the index to create. */
    var indexName: String by required()

    /** Data type of vectors stored in the index. */
    var dataType: VectorDataType by required()

    /** Number of dimensions in indexed vectors. */
    var dimension: Int by required()

    /** Distance metric used for similarity search. */
    var distanceMetric: DistanceMetric by required()

    /** Optional metadata indexing configuration. */
    var metadataConfiguration: MetadataConfiguration? = null

    internal fun build() = buildJsonObject {
        put("vectorBucketName", vectorBucketName)
        put("indexName", indexName)
        put("dataType", dataType.value)
        put("dimension", dimension)
        put("distanceMetric", distanceMetric.value)
        metadataConfiguration?.let { config ->
            put("metadataConfiguration", Json.encodeToJsonElement(config))
        }
    }

}
