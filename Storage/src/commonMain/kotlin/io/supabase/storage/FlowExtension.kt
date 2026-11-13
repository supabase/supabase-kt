package io.supabase.storage

import io.supabase.exceptions.HttpRequestException
import io.supabase.exceptions.RestException
import io.supabase.network.HttpRequestOverride
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private fun downloadOverride(flowProducer: ProducerScope<DownloadStatus>): HttpRequestOverride = {
    onDownload { bytesSentTotal, contentLength ->
        flowProducer.trySend(DownloadStatus.Progress(bytesSentTotal, contentLength ?: 0))
    }
}

private fun uploadOverride(flowProducer: ProducerScope<UploadStatus>): HttpRequestOverride = {
    onUpload { bytesSentTotal, contentLength ->
        flowProducer.trySend(UploadStatus.Progress(bytesSentTotal, contentLength ?: 0))
    }
}

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param options Additional options for the upload
 * @param data The data to upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.updateAsFlow(
    path: String,
    data: UploadData,
    options: UploadOptionBuilder.() -> Unit = {}
): Flow<UploadStatus> = uploadAsFlowRequest {
    update(path, data) {
        options()
        httpOverride(it)
    }
}

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param options Additional options for the upload
 * @param data The data to upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.uploadAsFlow(path: String, data: ByteArray, options: UploadOptionBuilder.() -> Unit = {}): Flow<UploadStatus> = uploadAsFlow(path, UploadData(
    ByteReadChannel(data), data.size.toLong()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The presigned url token
 * @param data The data to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.uploadToSignedUrlAsFlow(
    path: String,
    token: String,
    data: UploadData,
    options: UploadOptionBuilder.() -> Unit = {}
): Flow<UploadStatus> = uploadAsFlowRequest {
    uploadToSignedUrl(path, token, data) {
        options()
        httpOverride(it)
    }
}

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The presigned url token
 * @param data The data to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.uploadToSignedUrlAsFlow(path: String, token: String, data: ByteArray, options: UploadOptionBuilder.() -> Unit = {}): Flow<UploadStatus> = uploadToSignedUrlAsFlow(path, token, UploadData(ByteReadChannel(data), data.size.toLong()), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to update the file to
 * @param data The new data
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.uploadAsFlow(
    path: String,
    data: UploadData,
    options: UploadOptionBuilder.() -> Unit = {}
): Flow<UploadStatus> = uploadAsFlowRequest {
    upload(path, data) {
        options()
        httpOverride(it)
    }
}

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to update the file to
 * @param data The new data
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.updateAsFlow(path: String, data: ByteArray, options: UploadOptionBuilder.() -> Unit = {}): Flow<UploadStatus> = updateAsFlow(path, UploadData(ByteReadChannel(data), data.size.toLong()), options)

private fun BucketApi.uploadAsFlowRequest(
    producer: suspend (HttpRequestOverride) -> FileUploadResponse
) = callbackFlow {
    val key = producer(uploadOverride(this@callbackFlow))
    trySend(UploadStatus.Success(key))
    close()
}

/**
 * Downloads a file from [BucketApi.bucketId] under [path]
 * @param path The path to download
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the data as a byte array
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.downloadAuthenticatedAsFlow(
    path: String,
    options: DownloadOptionBuilder.() -> Unit = {}
): Flow<DownloadStatus> = downloadAsFlowRequest {
    downloadAuthenticated(path) {
        options()
        httpOverride(it)
    }
}

/**
 * Downloads a file from [BucketApi.bucketId] under [path] using the public url
 * @param path The path to download
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the data as a byte array
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.downloadPublicAsFlow(
    path: String,
    options: DownloadOptionBuilder.() -> Unit = {}
): Flow<DownloadStatus> =
    downloadAsFlowRequest {
        downloadPublic(path) {
            options()
            httpOverride(it)
        }
    }


/**
 * Downloads a file from [BucketApi.bucketId] under [path]
 * @param path The path to download
 * @param channel The channel to write the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the data as a byte array
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.downloadAuthenticatedAsFlow(
    path: String,
    channel: ByteWriteChannel,
    options: DownloadOptionBuilder.() -> Unit = {}
): Flow<DownloadStatus> = downloadAsFlowRequest {
    downloadAuthenticated(path, channel) {
        options()
        httpOverride(it)
    }
    null
}

/**
 * Downloads a file from [BucketApi.bucketId] under [path] using the public url
 * @param path The path to download
 * @param channel The channel to write the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the data as a byte array
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
fun BucketApi.downloadPublicAsFlow(
    path: String,
    channel: ByteWriteChannel,
    options: DownloadOptionBuilder.() -> Unit = {}
): Flow<DownloadStatus> = downloadAsFlowRequest {
    downloadPublic(path, channel) {
        options()
        httpOverride(it)
    }
    null
}

private fun BucketApi.downloadAsFlowRequest(
    producer: suspend (HttpRequestOverride) -> ByteArray?
) = callbackFlow {
    //If null, the data gets streamed to a channel, so we don't need to emit it later
    val data = producer(downloadOverride(this@callbackFlow))
    trySend(DownloadStatus.Success)
    if (data != null) {
        trySend(DownloadStatus.ByteData(data))
    }
    close()
}
