package io.supabase.auth.jwt

import io.supabase.auth.Auth
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Instant

/**
 * Represents a cache entry used in [JwkCache]
 * @param jwks A list of [JWK]s
 * @param cachedAt The time the entry was cached at
 */
data class JwkCacheEntry(
    val jwks: List<JWK>,
    val cachedAt: Instant
)

/**
 * Interface responsible to cache JWKs
 */
interface JwkCache {

    /**
     * Returns the current [JwkCacheEntry], or null if no cache entry was found
     */
    suspend fun get(): JwkCacheEntry?

    /**
     * Updates the current cache entry with [entry]
     */
    suspend fun set(entry: JwkCacheEntry)

}

/**
 * Caches JWKS values for all clients created in the same environment. This is
 * especially useful for shared-memory execution environments such as Vercel's
 * Fluid Compute, AWS Lambda or Supabase's Edge Functions. Regardless of how
 * many clients are created, if they share the same storage key they will use
 * the same JWKS cache, significantly speeding up [Auth.getClaims] with asymmetric
 * JWTs.
 */
object SharedJwkCache: JwkCache {

    private val entry: AtomicReference<JwkCacheEntry?> = AtomicReference(null)

    override suspend fun get(): JwkCacheEntry? = entry.load()

    override suspend fun set(entry: JwkCacheEntry) {
        this.entry.store(entry)
    }

}