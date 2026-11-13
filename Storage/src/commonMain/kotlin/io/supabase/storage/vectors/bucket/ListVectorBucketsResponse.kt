package io.supabase.storage.vectors.bucket

import io.supabase.serializer.mapValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * Response from listing vector buckets
 * @property vectorBuckets Array of bucket names
 * @property nextToken Token for fetching next page (if more results exist)
 */
@Serializable
data class ListVectorBucketsResponse(
    @SerialName("vectorBuckets")
    private val vectorBucketsArray: JsonArray,
    val nextToken: String? = null
) {

    val vectorBuckets by lazy { vectorBucketsArray.mapValue("vectorBucketName") }

}