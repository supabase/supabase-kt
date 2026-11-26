package io.supabase.auth

import io.supabase.auth.exception.NoSessionFoundException
import io.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import okio.Path

private val fileJson = Json {
    encodeDefaults = true
}

/**
 * A [SessionManager] that keeps the session in a JSON file only the current user can read. The default on Linux.
 *
 * @param file The file to keep the session in. Its directory is created when needed.
 * @param json The [Json] instance to use for serialization. **Important: [JsonBuilder.encodeDefaults] must be set to true.**
 */
class FileSessionManager(
    private val file: Path,
    private val json: Json = fileJson
) : SessionManager {

    override suspend fun saveSession(session: UserSession) {
        PrivateFiles.write(file, json.encodeToString(session))
    }

    override suspend fun loadSession(): UserSession {
        val session = PrivateFiles.read(file) ?: throw NoSessionFoundException()
        return json.decodeFromString(session)
    }

    override suspend fun deleteSession() {
        PrivateFiles.delete(file)
    }

    companion object {
        /**
         * The suffix of the default session file's name.
         */
        const val FILE_SUFFIX = "session.json"
    }
}
