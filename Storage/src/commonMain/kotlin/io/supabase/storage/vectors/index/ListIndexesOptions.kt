package io.supabase.storage.vectors.index

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Options for listing indexes within a bucket
 * @property vectorBucketName Name of the vector bucket containing indexes to list
 * @property prefix Filter indexes by name prefix
 * @property maxResults Maximum number of results to return (default: 100)
 * @property nextToken Token for pagination from previous response
 */
class ListIndexesOptions(
    val vectorBucketName: String
) {

    var prefix: String? = null
    var maxResults: Int? = null
    var nextToken: String? = null

    internal fun build() = buildJsonObject {
        prefix?.let { put("prefix", it) }
        maxResults?.let { put("maxResults", it) }
        nextToken?.let { put("nextToken", it) }
        put("vectorBucketName", vectorBucketName)
    }

}