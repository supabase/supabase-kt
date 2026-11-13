package io.supabase.storage.vectors.data

import kotlinx.serialization.Serializable

/**
 * Options for listing/scanning vectors in an index
 * Supports parallel scanning via segment configuration
 * @property vectorBucketName Name of the vector bucket
 * @property indexName Name of the index
 * @property maxResults Maximum number of results to return (default: 500, max: 1000)
 * @property nextToken Token for pagination from previous response
 * @property returnData Whether to include vector data in response
 * @property returnMetadata Whether to include metadata in response
 * @property segmentCount Total number of parallel segments (1-16)
 * @property segmentIndex Zero-based index of this segment (0 to segmentCount-1)
 */
@Serializable
class ListVectorsOptions(
    val vectorBucketName: String,
    val indexName: String
) {

    var maxResults: Int? = null
    var nextToken: String? = null
    var returnData: Boolean? = null
    var returnMetadata: Boolean? = null
    var segmentCount: Int? = null
    var segmentIndex: Int? = null

}