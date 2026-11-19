package io.supabase.auth

import kotlin.concurrent.atomics.AtomicReference

/**
 * A cache for the code verifier used in the PKCE flow.
 */
interface CodeVerifierCache {

    /**
     * Saves the given code verifier.
     */
    suspend fun saveCodeVerifier(codeVerifier: String)

    /**
     * Loads the saved code verifier from the cache.
     */
    suspend fun loadCodeVerifier(): String?

    /**
     * Deletes the saved code verifier from the cache.
     */
    suspend fun deleteCodeVerifier()

}

/**
 * A [CodeVerifierCache] that uses the [AtomicReference] API.
 */
class MemoryCodeVerifierCache(codeVerifier: String? = null): CodeVerifierCache {

    private val codeVerifier = AtomicReference(codeVerifier)

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        this.codeVerifier.store(codeVerifier)
    }

    override suspend fun loadCodeVerifier(): String? {
        return codeVerifier.load()
    }

    override suspend fun deleteCodeVerifier() {
        codeVerifier.store(null)
    }

}
