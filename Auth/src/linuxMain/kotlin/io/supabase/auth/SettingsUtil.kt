package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import io.supabase.logging.w

@SupabaseInternal
actual fun Auth.createDefaultSessionManager(): SessionManager {
    val directory = PrivateFiles.defaultDirectory() ?: return MemorySessionManager().also {
        logger.w { "Neither XDG_DATA_HOME nor HOME is set, so the session is kept in memory only" }
    }
    return FileSessionManager(directory / "${createDefaultSettingsKey(supabaseClient.supabaseUrl)}-${FileSessionManager.FILE_SUFFIX}")
}

@SupabaseInternal
actual fun Auth.createDefaultCodeVerifierCache(): CodeVerifierCache {
    val directory = PrivateFiles.defaultDirectory() ?: return MemoryCodeVerifierCache()
    return FileCodeVerifierCache(directory / "${createDefaultSettingsKey(supabaseClient.supabaseUrl)}-${FileCodeVerifierCache.FILE_SUFFIX}")
}
