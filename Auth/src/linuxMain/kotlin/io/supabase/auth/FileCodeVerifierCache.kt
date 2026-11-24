package io.supabase.auth

import okio.Path

/**
 * A [CodeVerifierCache] that keeps the code verifier in a file only the current user can read. The default on Linux.
 *
 * @param file The file to keep the code verifier in. Its directory is created when needed.
 */
class FileCodeVerifierCache(
    private val file: Path
) : CodeVerifierCache {

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        PrivateFiles.write(file, codeVerifier)
    }

    override suspend fun loadCodeVerifier(): String? = PrivateFiles.read(file)

    override suspend fun deleteCodeVerifier() {
        PrivateFiles.delete(file)
    }

    companion object {
        /**
         * The suffix of the default code verifier file's name.
         */
        const val FILE_SUFFIX = "code-verifier"
    }
}
