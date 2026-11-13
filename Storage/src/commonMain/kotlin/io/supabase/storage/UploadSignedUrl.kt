package io.supabase.storage

/**
 * A signed url to upload a file
 * @param url The signed url
 * @param path The path of the file
 * @param token The token to use for the upload
 */
data class UploadSignedUrl(
    val url: String,
    val path: String,
    val token: String
)
