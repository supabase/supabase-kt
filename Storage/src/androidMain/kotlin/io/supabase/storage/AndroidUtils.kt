package io.supabase.storage

import android.annotation.SuppressLint
import android.net.Uri
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param uri The uri to upload
 * @param options Additional options for the upload
 * @return the key to the updated file
 */
suspend fun BucketApi.upload(path: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = upload(path, UploadData(uri.readChannel(), uri.contentSize), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path]
 * @param path The path to upload the file to
 * @param uri The uri to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the updated file
 */
fun BucketApi.uploadAsFlow(path: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = uploadAsFlow(path, UploadData(uri.readChannel(), uri.contentSize), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param uri The uri to upload
 * @return the key to the updated file
 */
suspend fun BucketApi.uploadToSignedUrl(path: String, token: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrl(path, token, UploadData(uri.readChannel(), uri.contentSize), options)

/**
 * Uploads a file in [BucketApi.bucketId] under [path] using a presigned url
 * @param path The path to upload the file to
 * @param token The pre-signed url token
 * @param uri The uri to upload
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the updated file
 */
fun BucketApi.uploadToSignedUrlAsFlow(path: String, token: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = uploadToSignedUrlAsFlow(path, token, UploadData(uri.readChannel(), uri.contentSize), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to update the file to
 * @param uri The uri to update
 * @param options Additional options for the upload
 * @return the key to the updated file
 */
suspend fun BucketApi.update(path: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = update(path, UploadData(uri.readChannel(), uri.contentSize), options)

/**
 * Updates a file in [BucketApi.bucketId] under [path]
 * @param path The path to update the file to
 * @param uri The uri to update
 * @param options Additional options for the upload
 * @return A flow that emits the upload progress and at last the key to the updated file
 */
fun BucketApi.updateAsFlow(path: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = updateAsFlow(path, UploadData(uri.readChannel(), uri.contentSize), options)

@SuppressLint("Recycle") //toByteReadChannel closes the input stream automatically
private fun Uri.readChannel(): ByteReadChannel {
    val context = applicationContext()
    val inputStream = context.contentResolver.openInputStream(this) ?: throw IllegalArgumentException("Uri is not readable")
    return inputStream.toByteReadChannel()
}