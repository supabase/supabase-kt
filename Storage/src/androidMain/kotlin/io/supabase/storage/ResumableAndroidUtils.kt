package io.supabase.storage

import android.annotation.SuppressLint
import android.content.res.AssetFileDescriptor
import android.net.Uri
import io.supabase.storage.resumable.ResumableClient
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.discard
import io.ktor.utils.io.jvm.javaio.toByteReadChannel

/**
 * Creates a new upload or continues an existing one from the given [uri]
 * @param path The path to upload the file to
 * @param uri The uri of the file to upload (make sure you have access to it)
 * @param options The options for the upload
 */
suspend fun ResumableClient.createOrContinueUpload(path: String, uri: Uri, options: UploadOptionBuilder.() -> Unit = {}) = createOrContinueUpload(uri.createByteReader(), uri.toString(), uri.contentSize, path, options)

@SuppressLint("Recycle")
private fun Uri.createByteReader(): suspend (Long) -> ByteReadChannel = { offset: Long ->
    val context = applicationContext()
    val inputStream = context.contentResolver.openInputStream(this) ?: throw IllegalArgumentException("Uri is not readable")
    inputStream.toByteReadChannel().apply { discard(offset) }
}

internal val Uri.contentSize: Long
    @SuppressLint("Recycle")
    get() {
        return (applicationContext().contentResolver.openAssetFileDescriptor(this, "r") ?: error("Could not open file descriptor")).use(AssetFileDescriptor::getLength)
    }