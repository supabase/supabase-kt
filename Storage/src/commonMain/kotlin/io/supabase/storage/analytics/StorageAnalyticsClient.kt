package io.supabase.storage.analytics

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.exceptions.HttpRequestException
import io.supabase.safeBody
import io.supabase.storage.StorageListFilter
import io.supabase.storage.StorageRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Interface for managing Analytics Buckets using Iceberg tables
 */
interface StorageAnalyticsClient {

    /**
     * Creates a new analytics bucket using Iceberg tables
     * Analytics buckets are optimized for analytical queries and data processing
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param name A unique name for the bucket you are creating
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun createBucket(name: String): AnalyticBucket

    /**
     * Retrieves the details of all Analytics Storage buckets within an existing project
     * Only returns buckets of type 'ANALYTICS'
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param filter Query parameters for listing buckets
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun listBuckets(filter: StorageListFilter.Buckets.() -> Unit = {}): List<AnalyticBucket>

    /**
     * Deletes an existing analytics bucket
     * A bucket can't be deleted, if it is not empty
     * You must first empty the bucket before deletion
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @param name The unique identifier of the bucket you would like to delete
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun deleteBucket(name: String): String

    /**
     * @return The [IcebergClientConfiguration] with the base url and default headers to create a custom rest iceberg client.
     */
    suspend fun getIcebergClientConfiguration(): IcebergClientConfiguration

}

internal class StorageAnalyticsClientImpl(
    private val api: AuthenticatedSupabaseApi
) : StorageAnalyticsClient {

    override suspend fun createBucket(name: String): AnalyticBucket {
        return api.postJson("bucket", buildJsonObject {
            put("name", name)
        }).safeBody()
    }

    override suspend fun listBuckets(filter: StorageListFilter.Buckets.() -> Unit): List<AnalyticBucket> {
        val filter = StorageListFilter.Buckets().apply(filter)
        return api.get("bucket") {
            url.parameters.appendAll(filter.buildParameters())
        }.safeBody()
    }

    override suspend fun deleteBucket(name: String): String {
        return api.delete("bucket/$name").safeBody<JsonObject>()["message"]?.jsonPrimitive?.contentOrNull ?: error("Failed to delete bucket")
    }

    override suspend fun getIcebergClientConfiguration(): IcebergClientConfiguration {
        return IcebergClientConfiguration(
            api.resolveUrl(""),
            api::getDefaultHeaders
        )
    }

}