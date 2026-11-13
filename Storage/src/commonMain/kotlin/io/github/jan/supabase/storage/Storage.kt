package io.github.jan.supabase.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.AuthDependentPluginConfig
import io.github.jan.supabase.auth.api.AuthenticatedSupabaseApi
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.bodyOrNull
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.logging.createLogger
import io.github.jan.supabase.logging.w
import io.github.jan.supabase.plugins.CustomSerializationConfig
import io.github.jan.supabase.plugins.CustomSerializationPlugin
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.github.jan.supabase.safeBody
import io.github.jan.supabase.storage.Storage.Companion.DEFAULT_CHUNK_SIZE
import io.github.jan.supabase.storage.analytics.StorageAnalyticsClient
import io.github.jan.supabase.storage.analytics.StorageAnalyticsClientImpl
import io.github.jan.supabase.storage.resumable.ResumableCache
import io.github.jan.supabase.storage.resumable.createDefaultResumableCache
import io.github.jan.supabase.storage.vectors.StorageVectorsClient
import io.github.jan.supabase.storage.vectors.StorageVectorsClientImpl
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Plugin for interacting with the Supabase Storage API
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Storage)
 * }
 * ```
 *
 * then you have to interact with the Storage API like this:
 * ```kotlin
 * val bucket = supabase.storage.from("icons")
 * val bytes = bucket.downloadAuthenticated("icon.png")
 * ```
 */
interface Storage : MainPlugin<Storage.Config>, CustomSerializationPlugin {

    /**
     * Access analytics storage operations using Iceberg tables.
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @returns A [StorageAnalyticsClient] instance configured with the current storage settings.
     */
    @SupabaseExperimental
    val analytics: StorageAnalyticsClient

    /**
     * Access vector storage operations.
     *
     * **Public alpha:** This API is part of a public alpha release and may not be available to your account type.
     *
     * @returns A [StorageVectorsClient] instance configured with the current storage settings.
     */
    @SupabaseExperimental
    val vectors: StorageVectorsClient

    /**
     * The API used for network requests.
     */
    @SupabaseInternal
    val api: AuthenticatedSupabaseApi

    /**
     * Creates a new bucket in the storage
     * @param id the id of the bucket
     * @param builder overrides bucket config options (like whether the bucket should be public,
     * file size limit, etc.)
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun createBucket(id: String, builder: BucketBuilder.() -> Unit = {})

    /**
     * Updates a bucket in the storage
     * @param id the id of the bucket
     * @param builder the builder for the bucket
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun updateBucket(id: String, builder: BucketBuilder.() -> Unit = {})

    /**
     * Returns all buckets in the storage
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun listBuckets(filter: StorageListFilter.Buckets.() -> Unit = {}): List<Bucket>

    /**
     * Retrieves a bucket by its [bucketId]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun getBucket(bucketId: String): Bucket?

    /**
     * Empties a bucket by its [bucketId]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun emptyBucket(bucketId: String)

    /**
     * Deletes a bucket by its [bucketId]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues     */
    suspend fun deleteBucket(bucketId: String)

    /**
     * Builder function for interacting with a bucket with the given [bucketId]
     */
    operator fun get(bucketId: String): BucketApi

    /**
     * Builder function for interacting with a bucket with the given [bucketId]
     */
    fun from(bucketId: String): BucketApi = get(bucketId)

    /**
     * Purges the CDN cache for an entire bucket.
     *
     * Maps to `DELETE /cdn/{bucket}` on the Storage API. The server
     * issues a CDN invalidation for the bucket.
     *
     * **Requires the `service_role` key.** The underlying endpoint enforces
     * `service_role` JWT — calls made with the anon key or a user JWT will be
     * rejected by the server.
     *
     * **Hosted CDN feature.** On self-hosted Supabase, the Storage service must
     * have `CDN_PURGE_ENDPOINT_URL` configured and the `purgeCache` tenant
     * feature enabled, otherwise the server returns an error.
     *
     * @param id The unique identifier of the bucket you would like to purge from cache.
     * @param options Optional purge cache options.
     */
    suspend fun purgeBucketCache(
        id: String,
        options: PurgeCacheOptions.() -> Unit = {}
    )

    /**
     * Config for the storage plugin
     * @param transferTimeout the timeout for uploading and downloading files (default: 120 seconds)
     * @param resumable the resume settings to use for the storage api
     * @param serializer the serializer to use for the storage api, use null to use the default serializer
     */
    data class Config(
        var transferTimeout: Duration = 120.seconds,
        @PublishedApi internal var resumable: Resumable = Resumable(),
        override var serializer: SupabaseSerializer? = null,
        override var requireValidSession: Boolean = false,
    ) : MainConfig(), CustomSerializationConfig, AuthDependentPluginConfig {

        /**
         * @param cache the cache for caching resumable upload urls
         * @param retryTimeout the timeout for retrying resumable uploads when uploading a chunk fails
         * @param onlyUpdateStateAfterChunk whether the state should only be updated after a chunk was uploaded successfully or also when the chunk is currently being uploaded
         */
        data class Resumable(
            var cache: ResumableCache? = null,
            var retryTimeout: Duration = 5.seconds,
            var onlyUpdateStateAfterChunk: Boolean = false
        ) {

            /**
             * The default chunk size for resumable uploads. **Supabase currently only supports a chunk size of 6MB, so be careful when changing this value**
             */
            var defaultChunkSize: Long = DEFAULT_CHUNK_SIZE

        }

        /**
         * Config for resumable uploads
         */
        inline fun resumable(builder: Resumable.() -> Unit) {
            resumable = Resumable().apply(builder)
        }

    }

    companion object : SupabasePluginProvider<Config, Storage> {

        override val key: String = "storage"

        /**
         * The tag for the Storage logger.
         */
        const val LOGGING_TAG = "Supabase-Storage"

        /**
         * The api version of the storage plugin
         */
        const val API_VERSION = 1

        /**
         * The default chunk size for resumable uploads.
         */
        const val DEFAULT_CHUNK_SIZE = 6L * 1024L * 1024L

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)
        override fun create(supabaseClient: SupabaseClient, config: Config): Storage {
            return StorageImpl(supabaseClient, config)
        }

    }

}

internal class StorageImpl(override val supabaseClient: SupabaseClient, override val config: Storage.Config) : Storage {

    override val pluginKey: String
        get() = Storage.key

    override val apiVersion: Int
        get() = Storage.API_VERSION

    override val serializer: SupabaseSerializer = config.serializer ?: supabaseClient.defaultSerializer
    override val logger: SupabaseLogger = supabaseClient.createLogger(Storage.LOGGING_TAG, config)

    @OptIn(SupabaseInternal::class)
    override val api = supabaseClient.authenticatedSupabaseApi(this, defaultRequest = {
        timeout {
            requestTimeoutMillis = config.transferTimeout.inWholeMilliseconds
        }
    })

    private val resumableCache by lazy { config.resumable.cache ?: createDefaultResumableCache() }

    override val analytics: StorageAnalyticsClient = StorageAnalyticsClientImpl(api.resolve("iceberg"))
    override val vectors: StorageVectorsClient = StorageVectorsClientImpl(api.resolve("vector"))

    init {
        if(config.resumable.defaultChunkSize != DEFAULT_CHUNK_SIZE) {
            logger.w { "Supabase currently only supports a chunk size of 6MB" }
        }
    }

    override suspend fun listBuckets(filter: StorageListFilter.Buckets.() -> Unit): List<Bucket> {
        val response = api.get("bucket") {
            url.parameters.appendAll(StorageListFilter.Buckets().apply(filter).buildParameters())
        }
        return response.safeBody()
    }

    override suspend fun getBucket(bucketId: String): Bucket? = api.get("bucket/${storagePath(bucketId)}").safeBody()

    override suspend fun deleteBucket(bucketId: String) {
        api.delete("bucket/${storagePath(bucketId)}")
    }

    override suspend fun createBucket(id: String, builder: BucketBuilder.() -> Unit) {
        val bucketBuilder = BucketBuilder().apply(builder)
        val body = buildJsonObject {
            put("name", id)
            put("id", id)
            put("public", bucketBuilder.public ?: false)
            bucketBuilder.allowedMimeTypes?.let {
                put("allowed_mime_types", JsonArray(it.map { type -> JsonPrimitive(type) }))
            }
            bucketBuilder.fileSizeLimit?.let {
                put("file_size_limit", it.value)
            }
        }
        api.postJson("bucket", body)
    }

    override suspend fun updateBucket(id: String, builder: BucketBuilder.() -> Unit) {
        val bucketBuilder = BucketBuilder().apply(builder)
        val body = buildJsonObject {
            put("name", id)
            put("id", id)
            bucketBuilder.public?.let {
                put("public", bucketBuilder.public)
            }
            bucketBuilder.allowedMimeTypes?.let {
                put("allowed_mime_types", JsonArray(it.map { type -> JsonPrimitive(type) }))
            }
            bucketBuilder.fileSizeLimit?.let {
                put("file_size_limit", it.value)
            }
        }
        api.putJson("bucket/${storagePath(id)}", body)
    }

    override suspend fun emptyBucket(bucketId: String) {
        api.post("bucket/${storagePath(bucketId)}/empty")
    }

    override fun get(bucketId: String): BucketApi = BucketApiImpl(bucketId, this, api.resolve("object"), resumableCache)

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        val statusCode = response.status
        val error = supabaseClient.bodyOrNull<StorageErrorResponse>(response) ?: StorageErrorResponse(
            response.status.value,
            "Unknown error",
            "",
            ""
        )
        throw StorageRestException(
            error.error,
            error.message,
            response,
            error.statusCode,
            error.code ?: error.error
        )
    }

    override suspend fun purgeBucketCache(id: String, options: PurgeCacheOptions.() -> Unit) {
        val options = PurgeCacheOptions().apply(options)
        api.delete("cdn/${storagePath(id)}") {
            if(options.transformations) {
                parameter("transformations", true)
            }
        }
    }

}

/**
 * Supabase Storage is a simple way to store large files for various purposes
 */
val SupabaseClient.storage: Storage
    get() = pluginManager.getPlugin(Storage)
