package io.supabase.storage.vectors.data

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.exceptions.HttpRequestException
import io.supabase.safeBody
import io.supabase.storage.StorageRestException
import io.supabase.storage.vectors.data.VectorDataApi.Companion.SEGMENT_COUNT_RANGE
import io.supabase.storage.vectors.data.VectorDataApi.Companion.VECTOR_RANGE
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * Interface for vector data operations.
 */
interface VectorDataApi {

    /**
     * Inserts or updates [vectors] in batch (1-500 per request)
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun putVectors(vectors: List<VectorObject>)

    /**
     * Retrieves vectors by their keys in batch using the given [options]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun getVectors(options: GetVectorOptions.() -> Unit): List<VectorMatch>

    /**
     * Lists vectors in an index with pagination using the given [options]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun listVectors(options: ListVectorsOptions.() -> Unit = {}): ListVectorsResponse

    /**
     * Queries for similar vectors using approximate nearest neighbor search using the given [options]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun queryVectors(options: QueryVectorsOptions.() -> Unit): QueryVectorsResponse

    /**
     * Deletes vectors by their [keys] in batch (1-500 per request)
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun deleteVectors(keys: List<String>)

    companion object {

        /**
         * Range for vector deletion
         */
        val VECTOR_RANGE = 1..500

        /**
         * Range for the segment count
         */
        val SEGMENT_COUNT_RANGE = 1..16
    }

}

internal class VectorDataApiImpl(
    private val bucketName: String,
    private val indexName: String,
    private val api: AuthenticatedSupabaseApi
): VectorDataApi {

    override suspend fun putVectors(vectors: List<VectorObject>) {
        require(vectors.size in VECTOR_RANGE) {
            "Vector batch size must be between 1 and 500 items"
        }
        api.postJson("PutVectors", buildJsonObject {
            put("vectorBucketName", bucketName)
            put("indexName", indexName)
            put("vectors", Json.encodeToJsonElement(vectors))
        })
    }

    override suspend fun getVectors(options: GetVectorOptions.() -> Unit): List<VectorMatch> {
        val options = GetVectorOptions(bucketName, indexName).apply(options)
        val body = api.postJson("GetVectors", options).safeBody<JsonObject>()
        return Json.decodeFromJsonElement(body["vectors"] ?: error("No value with key 'vectors': $body"))
    }

    override suspend fun listVectors(options: ListVectorsOptions.() -> Unit): ListVectorsResponse {
        val options = ListVectorsOptions(bucketName, indexName).apply(options)
        if(options.segmentCount != null) {
            require(options.segmentCount in SEGMENT_COUNT_RANGE) {
                "segmentCount must be between 1 and 16"
            }
            if(options.segmentIndex != null) {
                require(options.segmentIndex in 0..<options.segmentCount!!) {
                    "segmentIndex must be between 0 and ${options.segmentCount!! - 1}"
                }
            }
        }
        return api.postJson("ListVectors", options).safeBody()
    }

    override suspend fun queryVectors(options: QueryVectorsOptions.() -> Unit): QueryVectorsResponse {
        return api.postJson("QueryVectors", QueryVectorsOptions(bucketName, indexName).apply(options).build()).safeBody()
    }

    override suspend fun deleteVectors(keys: List<String>) {
        require(keys.size in VECTOR_RANGE) {
            "Keys batch size must be between 1 and 500 items"
        }
        api.postJson("DeleteVectors", buildJsonObject {
            put("vectorBucketName", bucketName)
            put("indexName", indexName)
            put("keys", Json.encodeToJsonElement(keys))
        })
    }

}