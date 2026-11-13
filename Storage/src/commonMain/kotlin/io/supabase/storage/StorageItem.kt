package io.supabase.storage

/**
 * Represents a file in the storage bucket.
 * @param path The path of the file.
 * @param bucketId The id of the bucket.
 * @param authenticated Whether the file has to be accessed authenticated or not.
 */
data class StorageItem(
    val path: String,
    val bucketId: String,
    val authenticated: Boolean
)

/**
 * Creates a [StorageItem] for an authenticated file.
 */
fun authenticatedStorageItem(bucketId: String, path: String) = StorageItem(path, bucketId, true)

/**
 * Creates a [StorageItem] for a public file.
 */
fun publicStorageItem(bucketId: String, path: String) = StorageItem(path, bucketId, false)