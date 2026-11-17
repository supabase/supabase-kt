package io.supabase.storage.vectors.data

import kotlinx.serialization.Serializable

/**
 * Options for batch reading vectors
 * @property vectorBucketName Name of the vector bucket
 * @property indexName Name of the index
 * @property keys Array of vector keys to retrieve
 * @property returnData Whether to include vector data in response
 * @property returnMetadata Whether to include metadata in response
 */
@Serializable
class GetVectorOptions(
    val vectorBucketName: String,
    val indexName: String
) {

    val keys = mutableListOf<String>()
    var returnData: Boolean? = null
    var returnMetadata: Boolean? = null

}