package io.supabase.auth

/**
 * Applies minimal configuration to the [AuthConfig]. This is useful for server side applications, where you don't need to store the session or code verifier.
 * @see AuthConfigDefaults
 */
fun AuthConfigDefaults.minimalConfig() {
    this.alwaysAutoRefresh = false
    this.autoLoadFromStorage = false
    this.autoSaveToStorage = false
    this.sessionManager = MemorySessionManager()
    this.codeVerifierCache = MemoryCodeVerifierCache()
    this.enableLifecycleCallbacks = false
}

