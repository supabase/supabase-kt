package io.supabase.storage

import io.supabase.storage.resumable.Fingerprint
import io.supabase.storage.resumable.ResumableClient
import io.supabase.storage.resumable.ResumableUpload
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.discard
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize

/**
 * Creates a new resumable upload or continues an existing one.
 * If there is an url in the cache for the given [Fingerprint], the upload will be continued.
 * @param file The file to upload
 * @param path The path to upload the data to
 * @param options The options for the upload
 */
suspend fun ResumableClient.createOrContinueUpload(path: String, file: File, options: UploadOptionBuilder.() -> Unit = {}) = createOrContinueUpload({ file.readChannel().apply { discard(it) } }, file.absolutePath, file.length(), path, options)

/**
 * Creates a new resumable upload or continues an existing one.
 * If there is an url in the cache for the given [Fingerprint], the upload will be continued.
 * @param file The file to upload
 * @param path The path to upload the data to
 * @param options The options for the upload
 */
suspend fun ResumableClient.createOrContinueUpload(path: String, file: Path, options: UploadOptionBuilder.() -> Unit = {}) = createOrContinueUpload({ file.readChannel().apply { discard(it) } }, file.absolutePathString(), file.fileSize(), path, options)

/**
 * Reads pending uploads from the cache and creates a new [ResumableUpload] for each of them. This done in parallel, so you can start the uploads independently.
 */
suspend fun ResumableClient.continuePreviousFileUploads() =
    continuePreviousUploads { source, offset ->
        File(source).readChannel().apply { discard(offset) }
    }