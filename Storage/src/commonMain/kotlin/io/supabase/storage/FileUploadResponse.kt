package io.supabase.storage

/**
 * The response of a file upload
 * @param id The id of the file
 * @param path The path to the file. Can be used as is in [BucketApi] uploading methods
 * @param key The key of the file
 */
data class FileUploadResponse(
    val id: String? = null,
    val path: String,
    val key: String? = null
)
