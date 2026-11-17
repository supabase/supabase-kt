package io.supabase.storage.vectors.bucket

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Options for fetching vector buckets
 * @property prefix Filter buckets by name prefix
 * @property maxResults Maximum number of results to return (default: 100)
 * @property nextToken Token for pagination from previous response
 */
class VectorBucketFilter {

    var prefix: String? = null
    var maxResults: Int? = null
    var nextToken: String? = null

    internal fun build() = buildJsonObject {
        prefix?.let { put("prefix", it) }
        maxResults?.let { put("maxResults", it) }
        nextToken?.let { put("nextToken", it) }
    }

}