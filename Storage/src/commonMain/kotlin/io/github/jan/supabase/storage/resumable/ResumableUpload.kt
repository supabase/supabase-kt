package io.github.jan.supabase.storage.resumable

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.e
import io.github.jan.supabase.logging.w
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.FileUploadResponse
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.resumable.ResumableClient.Companion.TUS_VERSION
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.min

/**
 * Represents a resumable upload. Can be paused, resumed or cancelled.
 * The upload urls are automatically cashed, so you can resume the upload after your program crashed or the network reconnected without losing the upload progress.
 * You can customize the caching in [Storage.Config]
 */
sealed interface ResumableUpload {

    /**
     * The current upload progress as a [StateFlow]. The [UploadStatus] contains the total bytes sent and the total size of the upload. At the end of the upload, the [UploadStatus] will be [UploadStatus.Success].
     */
    val stateFlow: StateFlow<ResumableUploadState>

    /**
     * The upload fingerprint
     */
    val fingerprint: Fingerprint

    /**
     * Pauses this upload after the current chunk has been uploaded. Can be resumed using [startOrResumeUploading].
     * If the upload is already paused, this method does nothing.
     */
    suspend fun pause()

    /**
     * Cancels this upload and removes the upload url from the cache.
     */
    suspend fun cancel()

    /**
     * Starts or resumes this upload. Location url may be retrieved from the cache, so can start of after your program crashed or the network reconnected.
     */
    suspend fun startOrResumeUploading()

}

@Suppress("LongParameterList") //TODO: refactor
internal class ResumableUploadImpl(
    override val fingerprint: Fingerprint,
    private val path: String,
    private val cacheEntry: ResumableCacheEntry,
    private val createDataStream: suspend (Long) -> ByteReadChannel,
    private var offset: Long,
    private val chunkSize: Long,
    private val locationUrl: String,
    private val httpClient: HttpClient,
    private val storageApi: BucketApi,
    private val retrieveServerOffset: suspend () -> Long,
    private val removeFromCache: suspend () -> Unit,
    coroutineDispatcher: CoroutineDispatcher
): ResumableUpload {

    private val size = fingerprint.size

    private val paused = AtomicBoolean(false)
    private var serverOffset = 0L
    private val _stateFlow = MutableStateFlow<ResumableUploadState>(ResumableUploadState(fingerprint, cacheEntry, UploadStatus.Progress(offset, size), paused.load()))
    override val stateFlow: StateFlow<ResumableUploadState> = _stateFlow.asStateFlow()
    private val scope = CoroutineScope(coroutineDispatcher)
    private val config = storageApi.supabaseClient.storage.config.resumable
    private lateinit var dataStream: ByteReadChannel
    val logger = storageApi.supabaseClient.logger

    override suspend fun pause() {
        paused.store(true)
    }

    override suspend fun cancel() {
        scope.cancel()
        if(::dataStream.isInitialized) dataStream.cancel()
        removeFromCache()
    }

    override suspend fun startOrResumeUploading() {
        paused.compareAndSet(expectedValue = true, newValue = false)
        if(!::dataStream.isInitialized) dataStream = createDataStream(offset)
        scope.launch {
            var updateOffset = false
            while (offset < size) {
                if(paused.load() || !isActive) return@launch //check if paused or the scope is still active
                if(updateOffset) { //after an upload error we retrieve the server offset and update the data stream to avoid conflicts
                    logger.d { "Trying to update server offset for $path" }
                    try {
                        serverOffset = retrieveServerOffset() //retrieve server offset
                        offset = serverOffset
                        dataStream.cancel() //cancel old data stream as we are start reading from a new offset
                        dataStream = createDataStream(offset) //create new data stream
                    } catch(e: Exception) {
                        currentCoroutineContext().ensureActive()
                        logger.e(e) { "Error while updating server offset for $path. Retrying in ${config.retryTimeout}" }
                        delay(config.retryTimeout)
                        continue
                    }
                    updateOffset = false
                }
                try {
                    val uploaded = uploadChunk()
                    offset += uploaded
                } catch(e: Exception) {
                    currentCoroutineContext().ensureActive()
                    if(e !is IllegalStateException) {
                        logger.e(e) {"Error while uploading chunk. Retrying in ${config.retryTimeout}" }
                        delay(config.retryTimeout)
                        updateOffset = true //if an error occurs, we need to update the server offset to avoid conflicts
                        continue
                    }
                }
                _stateFlow.value = ResumableUploadState(fingerprint, cacheEntry, UploadStatus.Progress(offset, size), paused.load())
            }
            if(offset != serverOffset) error("Upload offset does not match server offset")
            _stateFlow.value = ResumableUploadState(fingerprint, cacheEntry, UploadStatus.Success(FileUploadResponse(path = path)), false)
            removeFromCache()
            dataStream.cancel()
        }
    }

    @OptIn(SupabaseInternal::class)
    private suspend fun uploadChunk(): Int {
        val limit = min(chunkSize, size - offset)
        val buffer = ByteArray(limit.toInt())
        dataStream.readFully(buffer, 0, limit.toInt())
        //dataStream.readFully(buffer, 0, limit.toInt())
        val uploadResponse = httpClient.patch(locationUrl) {
            header("Tus-Resumable", TUS_VERSION)
            header("Content-Type", "application/offset+octet-stream")
            header("Upload-Offset", offset)
            header("x-upsert", cacheEntry.upsert)
            bearerAuth(accessTokenOrApiKey())
            setBody(StreamContent(limit) {
                writeFully(buffer, 0, limit.toInt())
            })
            onUpload { bytesSentTotal, _ ->
                if(!config.onlyUpdateStateAfterChunk) {
                    _stateFlow.value = ResumableUploadState(fingerprint, cacheEntry, UploadStatus.Progress(offset + bytesSentTotal, size), paused.load())
                }
            }
        }
        when(uploadResponse.status) {
            HttpStatusCode.NoContent -> {
                logger.d { "Uploaded chunk" }
                serverOffset = uploadResponse.headers["Upload-Offset"]?.toLong() ?: error("No upload offset found")
            }
            HttpStatusCode.Conflict -> {
                logger.w { "Upload conflict, skipping chunk" }
                serverOffset = offset + limit
            }
            else -> error("Upload failed with status ${uploadResponse.status}. ${uploadResponse.bodyAsText()}")
        }
        return limit.toInt()
    }

    private fun accessTokenOrApiKey() = storageApi.supabaseClient.pluginManager.getPluginOrNull(Auth)?.currentAccessTokenOrNull() ?: storageApi.supabaseClient.supabaseKey

}