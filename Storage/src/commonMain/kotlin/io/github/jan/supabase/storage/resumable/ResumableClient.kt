@file:OptIn(ExperimentalEncodingApi::class, ExperimentalEncodingApi::class)

package io.github.jan.supabase.storage.resumable

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.UploadOptionBuilder
import io.github.jan.supabase.storage.resumable.ResumableClient.Companion.TUS_VERSION
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.discard
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Represents a resumable client. Can be used to create or continue resumable uploads.
 */
interface ResumableClient {

    /**
     * Creates a new resumable upload or continues an existing one.
     * If there is an url in the cache for the given [Fingerprint], the upload will be continued.
     * @param channel A function that takes the offset of the upload and returns a [ByteReadChannel] that reads the data to upload from the given offset
     * @param size The size of the data to upload
     * @param path The path to upload the data to
     * @param options The options for the upload
     */
    suspend fun createOrContinueUpload(channel: suspend (offset: Long) -> ByteReadChannel, source: String, size: Long, path: String, options: UploadOptionBuilder.() -> Unit = {}): ResumableUpload

    /**
     * Creates a new resumable upload or continues an existing one.
     * If there is an url in the cache for the given [Fingerprint], the upload will be continued.
     * @param data The data to upload as a [ByteArray]
     * @param path The path to upload the data to
     * @param options The options for the upload
     */
    @Suppress("unused")
    suspend fun createOrContinueUpload(data: ByteArray, source: String, path: String, options: UploadOptionBuilder.() -> Unit = {}) = createOrContinueUpload({ ByteReadChannel(data).apply { discard(it) } }, source, data.size.toLong(), path, options)

    /**
     * Reads pending uploads from the cache and creates a new [ResumableUpload] for each of them. This done in parallel, so you can start the downloads independently.
     * @param channelProducer A function that takes the source of the upload (e.g. file path) plus the channel offset (you can use [ByteReadChannel.discard] for that) and returns a [ByteReadChannel] for the data to upload
     */
    suspend fun continuePreviousUploads(channelProducer: suspend (source: String, offset: Long) -> ByteReadChannel): List<Deferred<ResumableUpload>>

    companion object {

        /**
         * The TUS (resumable upload protocol) version to use
         */
        const val TUS_VERSION = "1.0.0"
    }

}

internal class ResumableClientImpl(private val storageApi: BucketApi, internal val cache: ResumableCache): ResumableClient {

    @OptIn(SupabaseInternal::class)
    private val httpClient = storageApi.supabaseClient.httpClient.httpClient
    private val url = storageApi.supabaseClient.storage.resolveUrl("upload/resumable")
    private val chunkSize = storageApi.supabaseClient.storage.config.resumable.defaultChunkSize
    val logger = storageApi.supabaseClient.logger

    override suspend fun continuePreviousUploads(channelProducer: suspend (source: String, offset: Long) -> ByteReadChannel): List<Deferred<ResumableUpload>> {
        val cachedEntries = cache.entries()
        return cachedEntries.map { (fingerprint, cacheEntry) ->
            logger.d { "Found cached upload for ${cacheEntry.path}" }
            coroutineScope {
                async {
                    resumeUpload({ channelProducer(fingerprint.source, it) }, cacheEntry, fingerprint.source, cacheEntry.path, fingerprint.size)
                }
            }
        }
    }

    override suspend fun createOrContinueUpload(
        channel: suspend (offset: Long) -> ByteReadChannel,
        source: String,
        size: Long,
        path: String,
        options: UploadOptionBuilder.() -> Unit
    ): ResumableUpload {
        val cachedEntry = cache.get(Fingerprint(source, size))
        if(cachedEntry != null) {
            logger.d { "Found cached upload for $path" }
            return resumeUpload(channel, cachedEntry, source, path, size)
        }
        return createUpload(channel, source, path, size, options)
    }

    private suspend fun createUpload(channel: suspend (Long) -> ByteReadChannel, source: String, path: String, size: Long, options: UploadOptionBuilder.() -> Unit): ResumableUploadImpl {
        val uploadOptions = UploadOptionBuilder(storageApi.supabaseClient.storage.serializer).apply(options)
        val response = httpClient.post(url) {
            header("Upload-Metadata", encodeMetadata(createMetadata(path, uploadOptions.contentType)))
            bearerAuth(accessTokenOrApiKey())
            header("Upload-Length", size)
            header("Tus-Resumable", TUS_VERSION)
            header("x-upsert", uploadOptions.upsert)
        }
        when(response.status) {
            HttpStatusCode.Conflict -> error("Specified path already exists. Consider setting upsert to true")
            else -> {
                if(!response.status.isSuccess()) error("Upload failed with status ${response.status}. Message: ${response.bodyAsText()}")
            }
        }
        val uploadUrl = response.headers["Location"] ?: error("No upload url found")
        val fingerprint = Fingerprint(source, size)
        val cacheEntry = ResumableCacheEntry(uploadUrl, path, storageApi.bucketId, Clock.System.now() + 1.days, uploadOptions.upsert, uploadOptions.contentType.toString())
        cache.set(fingerprint, cacheEntry)
        return ResumableUploadImpl(
            fingerprint = fingerprint,
            path = path,
            cacheEntry = cacheEntry,
            createDataStream = channel,
            offset = 0,
            chunkSize = chunkSize,
            locationUrl = uploadUrl,
            httpClient = httpClient,
            storageApi = storageApi,
            retrieveServerOffset = { retrieveServerOffset(uploadUrl, path) },
            removeFromCache = { cache.remove(fingerprint) },
            coroutineDispatcher = storageApi.supabaseClient.coroutineDispatcher
        )
    }

    private suspend fun resumeUpload(channel: suspend (Long) -> ByteReadChannel, entry: ResumableCacheEntry, source: String, path: String, size: Long): ResumableUploadImpl {
        val fingerprint = Fingerprint(source, size)
        if(Clock.System.now() > entry.expiresAt) {
            logger.d { "Upload url for $path expired. Creating new one" }
            cache.remove(fingerprint)
            return createUpload(channel, source, path, size) {
                upsert = entry.upsert
                contentType = ContentType.parse(entry.contentType)
            }
        }
        val offset = retrieveServerOffset(entry.url, path)
        if(offset < size) {
            return ResumableUploadImpl(
                fingerprint = fingerprint,
                path = path,
                cacheEntry = entry,
                createDataStream = channel,
                offset = offset,
                chunkSize = chunkSize,
                locationUrl = entry.url,
                httpClient = httpClient,
                storageApi = storageApi,
                retrieveServerOffset = { retrieveServerOffset(entry.url, path)},
                removeFromCache = { cache.remove(fingerprint) },
                coroutineDispatcher = storageApi.supabaseClient.coroutineDispatcher
            )
        } else error("File already uploaded")
    }

    private suspend fun retrieveServerOffset(url: String, path: String): Long {
        val response = httpClient.request(url) {
            method = HttpMethod.Head
            bearerAuth(accessTokenOrApiKey())
            header("Tus-Resumable", TUS_VERSION)
        }
        if(!response.status.isSuccess()) error("Failed to retrieve server offset: ${response.status} ${response.bodyAsText()}")
        val offset = response.headers["Upload-Offset"]?.toLongOrNull() ?: error("No upload offset found")
        logger.d { "Server offset for $path is $offset" }
        return offset
    }

    private fun accessTokenOrApiKey() = storageApi.supabaseClient.pluginManager.getPluginOrNull(Auth)?.currentAccessTokenOrNull() ?: storageApi.supabaseClient.supabaseKey

    private fun createMetadata(path: String, contentType: ContentType? = null): Map<String, String> = buildMap {
        put("bucketName", storageApi.bucketId)
        put("objectName", path)
        put("contentType", contentType?.toString() ?: ContentType.defaultForFilePath(path).toString())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeMetadata(metadata: Map<String, String>): String {
        return metadata.entries.joinToString(",") { (key, value) ->
            key + " " + Base64.encode(value.toByteArray())
        }
    }

}
