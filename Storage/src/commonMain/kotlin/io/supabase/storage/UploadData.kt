package io.supabase.storage

import io.ktor.utils.io.ByteReadChannel

/**
 * Represents the data to upload
 * @param stream The [ByteReadChannel] for streaming the data
 * @param size The size of the data
 */
data class UploadData(val stream: ByteReadChannel, val size: Long)