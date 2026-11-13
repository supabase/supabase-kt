package io.supabase.storage.vectors.index

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.exceptions.HttpRequestException
import io.supabase.safeBody
import io.supabase.storage.StorageRestException
import io.supabase.storage.vectors.data.VectorDataApi
import io.supabase.storage.vectors.data.VectorDataApiImpl
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Interface for index management and access to vector operations.
 */
interface VectorIndexApi {

    /**
     * Creates a new vector index in this bucket
     * Convenience method that automatically includes the bucket name
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param options - Index configuration (vectorBucketName is automatically set)
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun createIndex(options: CreateIndexOptions.() -> Unit)

    /**
     * Lists indexes in this bucket
     * Convenience method that automatically includes the bucket name
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param options Listing options (vectorBucketName is automatically set)
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun listIndexes(options: ListIndexesOptions.() -> Unit = {}): ListIndexesResponse

    /**
     * Retrieves metadata for a specific index in this bucket
     * Convenience method that automatically includes the bucket name
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param indexName Name of the index to retrieve
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun getIndex(indexName: String): VectorIndex

    /**
     * Deletes an index from this bucket
     * Convenience method that automatically includes the bucket name
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param indexName Name of the index to delete
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun deleteIndex(indexName: String)

    /**
     * Access operations for a specific index within this bucket
     * Returns a scoped client for vector data operations
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param indexName Name of the index
     * @returns Index-scoped client with vector data operations
     */
    fun index(indexName: String): VectorDataApi

}

internal class VectorIndexApiImpl(
    private val bucketName: String,
    private val api: AuthenticatedSupabaseApi
): VectorIndexApi {

    override suspend fun createIndex(options: CreateIndexOptions.() -> Unit) {
        api.postJson("CreateIndex", CreateIndexOptions(bucketName).apply(options).build())
    }

    override suspend fun getIndex(indexName: String): VectorIndex {
        val responseBody = api.postJson("GetIndex", buildJsonObject {
            put("vectorBucketName", bucketName)
            put("indexName", indexName)
        }).safeBody<JsonObject>()
        return Json.decodeFromJsonElement(responseBody["index"]?.jsonObject ?: error("Invalid response body: $responseBody"))
    }

    override suspend fun listIndexes(options: ListIndexesOptions.() -> Unit): ListIndexesResponse {
        val options = ListIndexesOptions(bucketName).apply(options)
        return api.postJson("ListIndexes", options.build()).safeBody()
    }

    override suspend fun deleteIndex(indexName: String) {
        api.postJson("DeleteIndex", buildJsonObject {
            put("vectorBucketName", bucketName)
            put("indexName", indexName)
        })
    }

    override fun index(indexName: String): VectorDataApi {
        return VectorDataApiImpl(bucketName, indexName, api)
    }

}