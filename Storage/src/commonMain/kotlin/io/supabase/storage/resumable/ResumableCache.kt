
package io.supabase.storage.resumable

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Represents a cache entry for a resumable upload
 * @param url The upload url
 * @param path The storage path
 * @param bucketId The bucket id
 * @param expiresAt The time the url expires
 * @param upsert Whether the entry should be updated if it already exists
 * @param contentType The content type of the file
 */
@Serializable
data class ResumableCacheEntry(
    val url: String,
    val path: String,
    val bucketId: String,
    val expiresAt: Instant,
    val upsert: Boolean = false,  //for compatibility with the old cache
    val contentType: String = "application/octet-stream"
)

/**
 * A pair of a [Fingerprint] and a [ResumableCacheEntry]
 */
typealias CachePair = Pair<Fingerprint, ResumableCacheEntry>

/**
 * A cache for storing resumable upload urls
 */
interface ResumableCache {

    /**
     * Stores the [entry] for the [fingerprint]. The fingerprint consists of the bucket id, the file path and the file size
     */
    suspend fun set(fingerprint: Fingerprint, entry: ResumableCacheEntry)

    /**
     * Returns the resumable cache entry for the [fingerprint] or null if no url was stored
     */
    suspend fun get(fingerprint: Fingerprint): ResumableCacheEntry?

    /**
     * Removes the entry for the [fingerprint]
     */
    suspend fun remove(fingerprint: Fingerprint)

    /**
     * Clears the cache
     */
    suspend fun clear()

    /**
     * Returns all entries in the cache
     */
    suspend fun entries(): List<CachePair>

}

@SupabaseInternal
expect fun createDefaultResumableCache(): ResumableCache