package io.supabase.storage.vectors.data

import io.supabase.dsl.required
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Options for querying similar vectors (ANN search)
 * @property vectorBucketName Name of the vector bucket
 * @property indexName Name of the index
 * @property queryVector Query vector to find similar vectors
 * @property topK Number of nearest neighbors to return (default: 10)
 * @property filter Optional JSON filter for metadata
 * @property returnDistance Whether to include distance scores
 * @property returnMetadata Whether to include metadata in results
 */
class QueryVectorsOptions(
    val vectorBucketName: String,
    val indexName: String
) {

    var queryVector: VectorData by required()
    var topK: Int? = null
    var filter: JsonObject? = null
    var returnDistance: Boolean? = null
    var returnMetadata: Boolean? = null

    internal fun build() = buildJsonObject {
        put("vectorBucketName", vectorBucketName)
        put("indexName", indexName)
        put("queryVector", Json.encodeToJsonElement(queryVector))
        topK?.let { put("topK", it) }
        filter?.let { put("filter", it) }
        returnDistance?.let { put("returnDistance", it) }
        returnMetadata?.let { put("returnMetadata", it) }
    }

}