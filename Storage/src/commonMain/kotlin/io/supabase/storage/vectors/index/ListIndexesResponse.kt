package io.supabase.storage.vectors.index

import io.supabase.serializer.mapValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * Response from listing indexes
 * @property indexes - Array of index names
 * @property nextToken - Token for fetching next page (if more results exist)
 */
@Serializable
data class ListIndexesResponse(
    @SerialName("indexes") private val indexesRaw: JsonArray,
    val nextToken: String? = null
) {

    val indexes by lazy { indexesRaw.mapValue("indexName") }

}
