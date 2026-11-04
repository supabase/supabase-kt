package io.supabase.storage

import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.writeBytes

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return the key to the uploaded file
 */
suspend fun BucketApi.upload(path: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = upload(path, UploadData(file.readChannel(), file.length()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.uploadAsFlow(path: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = uploadAsFlow(path, UploadData(file.readChannel(), file.length()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return the key to the uploaded file
 */
suspend fun BucketApi.upload(path: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = upload(path, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.uploadAsFlow(path: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = uploadAsFlow(path, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return the key to the uploaded file
 */
suspend fun BucketApi.uploadToSignedUrl(path: String, token: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrl(path, token, UploadData(file.readChannel(), file.length()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.uploadToSignedUrlAsFlow(path: String, token: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrlAsFlow(path, token, UploadData(file.readChannel(), file.length()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param file The file to upload
 * @param options Additional options for the upload
 */
suspend fun BucketApi.uploadToSignedUrl(path: String, token: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrl(path, token, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param file The file to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.uploadToSignedUrlAsFlow(path: String, token: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrlAsFlow(path, token, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to be updated
 * @param file The new file
 * @param options Additional options for the upload
 */
suspend fun BucketApi.update(path: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = update(path, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to be updated
 * @param file The new file
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.updateAsFlow(path: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = updateAsFlow(path, UploadData(file.readChannel(), file.fileSize()), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to be updated
 * @param file The new file
 * @param options Additional options for the upload
 */
suspend fun BucketApi.update(path: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = update(path, UploadData(file.readChannel(), file.length()), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to be updated
 * @param file The new file
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the uploaded file
 */
fun BucketApi.updateAsFlow(path: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = updateAsFlow(path, UploadData(file.readChannel(), file.length()), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 */
suspend fun BucketApi.downloadAuthenticatedTo(path: String, file: File, options: DownloadOptionBuilder.() -> Unit = {}) = downloadAuthenticated(path, file.writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the key to the downloaded file
 */
fun BucketApi.downloadAuthenticatedToAsFlow(path: String, file: File, options: DownloadOptionBuilder.() -> Unit = {}) = downloadAuthenticatedAsFlow(path, file.writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 */
suspend fun BucketApi.downloadAuthenticatedTo(path: String, file: Path, options: DownloadOptionBuilder.() -> Unit = {}) = downloadAuthenticated(path, file.toFile().writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the key to the downloaded file
 */
fun BucketApi.downloadAuthenticatedToAsFlow(path: String, file: Path, options: DownloadOptionBuilder.() -> Unit = {}) = downloadAuthenticatedAsFlow(path, file.toFile().writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 */
suspend fun BucketApi.downloadPublicTo(path: String, file: File, options: DownloadOptionBuilder.() -> Unit = {}) = downloadPublic(path, file.writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the key to the downloaded file
 */
fun BucketApi.downloadPublicToAsFlow(path: String, file: File, options: DownloadOptionBuilder.() -> Unit = {}) = downloadPublicAsFlow(path, file.writeChannel(), options)

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 */
suspend fun BucketApi.downloadPublicTo(path: String, file: Path, options: DownloadOptionBuilder.() -> Unit = {}) {
    val bytes = downloadPublic(path, options)
    file.writeBytes(bytes)
}

/**
 * Downloads a file from [BucketApi.bucketId] under [path] and saves it to [file]
 * @param path The path to download the file from
 * @param file The file to save the data to
 * @param options Additional options for the download
 * @return A flow that emits the download progress and at last the key to the downloaded file
 */
fun BucketApi.downloadPublicToAsFlow(path: String, file: Path, options: DownloadOptionBuilder.() -> Unit = {}) = downloadPublicAsFlow(path, file.toFile().writeChannel(), options)