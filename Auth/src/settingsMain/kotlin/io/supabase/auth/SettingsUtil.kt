package io.supabase.auth

import com.russhwolf.settings.Settings
import io.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_NODE

@SupabaseInternal
fun createDefaultSettings() = try {
    Settings()
} catch(e: Exception) {
    error("Failed to create default settings for SettingsSessionManager. You might have to provide a custom settings instance or a custom session manager. Learn more at https://github.com/supabase-community/supabase-kt/wiki/Session-Saving")
}

@SupabaseInternal
fun createDefaultSettingsKey(supabaseUrl: String) =
    "sb-${supabaseUrl.removeSuffix("/").replace('/', '-').replace('.', '-')}"

@SupabaseInternal
actual fun Auth.createDefaultSessionManager(): SessionManager =
    if(!IS_NODE)
        SettingsSessionManager(
            key = "${createDefaultSettingsKey(supabaseClient.supabaseUrl)}-${SettingsSessionManager.SETTINGS_KEY}",
        )
    else
        MemorySessionManager()
@SupabaseInternal
actual fun Auth.createDefaultCodeVerifierCache(): CodeVerifierCache =
    if(!IS_NODE)
        SettingsCodeVerifierCache(
            key = "${createDefaultSettingsKey(supabaseClient.supabaseUrl)}-${SettingsCodeVerifierCache.SETTINGS_KEY}",
        )
    else
        MemoryCodeVerifierCache()