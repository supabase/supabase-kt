package io.supabase.storage

import io.supabase.SupabaseClient
import io.supabase.auth.resolveAccessToken
import io.supabase.exceptions.HttpRequestException
import io.supabase.exceptions.RestException
import io.supabase.storage.resumable.ResumableClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlin.time.Duration

/**
 * The api for interacting with a bucket
 */
interface BucketApi {

    /**
     * The id of the bucket
     */
    val bucketId: String

    /**
     * The current [SupabaseClient]
     */
    val supabaseClient: SupabaseClient

    /**
     * The client for interacting with the resumable upload api
     */
    val resumable: ResumableClient

    /**
     * Set an HTTP header for the request.
     * Creates a shallow copy of headers to avoid mutating shared state.
     * @param name Header name
     * @param value Header value
     * @returns this - For method chaining
     */
    fun setHeader(name: String, value: String): BucketApi

    /**
     * Uploads a file in [bucketId] under [path]
     * @param path The path to upload the file to
     * @param data The data to upload
     * @param options Additional options for the upload
     * @return the key to the uploaded file
     * @throws IllegalArgumentException if data to upload is empty
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun upload(path: String, data: ByteArray, options: UploadOptionBuilder.() -> Unit = {}): FileUploadResponse {
        require(data.isNotEmpty()) { "The data to upload should not be empty" }
        return upload(path, UploadData(ByteReadChannel(data), data.size.toLong()), options)
    }

    /**
     * Uploads a file in [bucketId] under [path]
     * @param path The path to upload the file to
     * @param data The data to upload
     * @param options Additional options for the upload
     * @return the key to the uploaded file
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun upload(path: String, data: UploadData, options: UploadOptionBuilder.() -> Unit = {}): FileUploadResponse

    /**
     * Uploads a file in [bucketId] under [path] using a pre-signed url
     * @param path The path to upload the file to
     * @param token The pre-signed url token
     * @param data The data to upload
     * @param options Additional options for the upload
     * @return the key of the uploaded file
     * @throws IllegalArgumentException if data to upload is empty
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun uploadToSignedUrl(
        path: String,
        token: String,
        data: ByteArray,
        options: UploadOptionBuilder.() -> Unit = {}
    ): FileUploadResponse {
        require(data.isNotEmpty()) { "The data to upload should not be empty" }
        return uploadToSignedUrl(path, token, UploadData(ByteReadChannel(data), data.size.toLong()), options)
    }

    /**
     * Uploads a file in [bucketId] under [path] using a presigned url
     * @param path The path to upload the file to
     * @param token The presigned url token
     * @param data The data to upload
     * @param options Additional options for the upload
     * @return the key of the uploaded file
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     * @throws HttpRequestException on network related issues
     */
    suspend fun uploadToSignedUrl(path: String, token: String, data: UploadData, options: UploadOptionBuilder.() -> Unit = {}): FileUploadResponse

    /**
     * Updates a file in [bucketId] under [path]
     * @param path The path to update the file to
     * @param data The new data
     * @param options Additional options for the upload
     * @return the key to the updated file
     * @throws IllegalArgumentException if data to upload is empty
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun update(path: String, data: ByteArray, options: UploadOptionBuilder.() -> Unit = {}): FileUploadResponse {
        require(data.isNotEmpty()) { "The data to upload should not be empty" }
        return update(path, UploadData(ByteReadChannel(data), data.size.toLong()), options)
    }

    /**
     * Updates a file in [bucketId] under [path]
     * @param path The path to update the file to
     * @param data The new data
     * @param options Additional options for the upload
     * @return the key to the updated file
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun update(path: String, data: UploadData, options: UploadOptionBuilder.() -> Unit = {}): FileUploadResponse

    /**
     * Deletes all files in [bucketId] with in [paths]
     * @param paths The paths to delete
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun delete(paths: Collection<String>)

    /**
     * Deletes all files in [bucketId] with in [paths]
     * @param paths The paths to delete
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun delete(vararg paths: String) = delete(paths.toList())

    /**
     * Moves a file under [from] to [to]
     * @param from The path to move from
     * @param to The path to move to
     * @param destinationBucket The bucket to move the file to. If null, the file will be moved within the same bucket
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun move(from: String, to: String, destinationBucket: String? = null)

    /**
     * Copies a file under [from] to [to]
     * @param from The path to copy from
     * @param to The path to copy to
     * @param destinationBucket The destination bucket to copy to. If null, the current bucket is used
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun copy(from: String, to: String, destinationBucket: String? = null)

    /**
     * Creates a signed url to upload without authentication.
     * These urls are valid for 2 hours.
     * @param path The path to create an url for
     * @param upsert Whether to upsert the file if it already exists
     */
    suspend fun createSignedUploadUrl(path: String, upsert: Boolean = false): UploadSignedUrl

    /**
     * Creates a signed url to download without authentication. The url will expire after [expiresIn]
     * @param path The path to create a url for
     * @param expiresIn The duration the url is valid
     * @param builder Optional modifier to add a [transformation][SignedUrlBuilder.transformation],
     * [force-download][SignedUrlBuilder.download] or [cacheNonce][BucketUrlBuilder.cacheNonce] to the created url
     * @return The url to download the file
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun createSignedUrl(path: String, expiresIn: Duration, builder: SignedUrlBuilder.() -> Unit = {}): String

    /**
     * Creates signed urls for all specified paths. The urls will expire after [expiresIn]
     * @param expiresIn The duration the urls are valid
     * @param paths The paths to create urls for
     * @param builder Optional modifier to add a [force-download][SignedUrlsBuilder.download] or [cacheNonce][BucketUrlBuilder.cacheNonce] to the created urls
     * @return A list of [SignedUrl]s
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun createSignedUrls(expiresIn: Duration, paths: Collection<String>, builder: SignedUrlsBuilder.() -> Unit = {}): List<SignedUrl>

    /**
     * Creates signed urls for all specified paths. The urls will expire after [expiresIn]
     * @param expiresIn The duration the urls are valid
     * @param paths The paths to create urls for
     * @param builder Optional modifier to add a [force-download][SignedUrlsBuilder.download] or [cacheNonce][BucketUrlBuilder.cacheNonce] to the created urls
     * @return A list of [SignedUrl]s
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun createSignedUrls(expiresIn: Duration, vararg paths: String, builder: SignedUrlsBuilder.() -> Unit = {}) = createSignedUrls(expiresIn, paths.toList(), builder)

    /**
     * Downloads a file from [bucketId] under [path]
     * @param path The path to download
     * @param options Additional options for the download
     * @return The file as a byte array
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun downloadAuthenticated(path: String, options: DownloadOptionBuilder.() -> Unit = {}): ByteArray

    /**
     * Downloads a file from [bucketId] under [path]
     * @param path The path to download
     * @param channel The channel to write the data to
     * @param options Additional options for the download
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun downloadAuthenticated(path: String, channel: ByteWriteChannel, options: DownloadOptionBuilder.() -> Unit = {})

    /**
     * Downloads a file from [bucketId] under [path] using the public url
     * @param path The path to download
     * @param options Additional options for the download
     * @return The file as a byte array
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun downloadPublic(path: String, options: DownloadOptionBuilder.() -> Unit = {}): ByteArray

    /**
     * Downloads a file from [bucketId] under [path] using the public url
     * @param path The path to download
     * @param channel The channel to write the data to
     * @param options Additional options for the download
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun downloadPublic(path: String, channel: ByteWriteChannel, options: DownloadOptionBuilder.() -> Unit = {})


    /**
     * Searches for files with the given [prefix] and [filter]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun list(
        prefix: String = "",
        filter: StorageListFilter.Files.() -> Unit = {}
    ): List<FileObject>

    /**
     * Returns information about the file under [path]
     * @param path The path to get information about
     * @return The file object
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun info(path: String): FileObjectV2

    /**
     * Checks if a file exists under [path]
     * @return true if the file exists, false otherwise
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun exists(path: String): Boolean

    /**
     * Changes the bucket's public status to [public]
     * @throws StorageRestException containing an error code, if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun changePublicStatusTo(public: Boolean)

    /**
     * Purges the CDN cache for a single object in this bucket.
     *
     * Maps to `DELETE /cdn/{bucket}/{path}` on the Storage API. The server
     * issues a CDN invalidation for the object and returns `{ message: 'success' }`.
     *
     * **Requires the `service_role` key.** The underlying endpoint enforces
     * `service_role` JWT — calls made with the anon key or a user JWT will be
     * rejected by the server.
     *
     * **Hosted CDN feature.** On self-hosted Supabase, the Storage service must
     * have `CDN_PURGE_ENDPOINT_URL` configured and the `purgeCache` tenant
     * feature enabled, otherwise the server returns an error.
     *
     * Operates on a single object path. There is no wildcard or recursion: pass
     * the exact path of the object you want invalidated.
     *
     * @param path The path (relative to the bucket) of the object to purge, e.g. `folder/avatar.png`.
     * @param options Optional purge cache options.
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun purgeCache(
        path: String,
        options: PurgeCacheOptions.() -> Unit = {}
    )

    /**
     * Returns the public url of [path]
     */
    fun publicUrl(path: String, builder: PublicUrlBuilder.() -> Unit = {}): String

    /**
     * Returns the authenticated url of [path]. Requires bearer token authentication using the user's access token
     */
    fun authenticatedUrl(path: String): String

    /**
     * Returns the authenticated render url of [path] with the given [transform]
     */
    fun authenticatedRenderUrl(path: String, transform: ImageTransformation.() -> Unit = {}): String

    /**
     * Returns the public render url of [path] with the given [transform]
     */
    @Deprecated("Use publicUrl instead", ReplaceWith("publicUrl"))
    fun publicRenderUrl(path: String, transform: ImageTransformation.() -> Unit = {}): String = publicUrl(path) {
        transform(transform)
    }

    companion object {

        /**
         * The header to use for upserting files
         */
        const val UPSERT_HEADER = "x-upsert"

        /**
         * The header to use for the user metadata
         */
        const val METADATA_HEADER = "x-metadata"

    }

}


/**
 * Can be used if you want to quickly access a file under an **url** with your **auth_token** using a custom download method.
 *
 *
 * To interact with files which require authentication use the provided access token and add it to the Authorization header:
 *
 * **Authentication: Bearer <your_access_token>**
 * @param path The path to download
 */
suspend fun BucketApi.authenticatedRequest(path: String): Pair<String, String> {
    val url = authenticatedUrl(path)
    val token = supabaseClient.resolveAccessToken(supabaseClient.storage) ?: error("No access token available")
    return token to url
}