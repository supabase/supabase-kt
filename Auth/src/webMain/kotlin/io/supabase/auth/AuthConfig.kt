package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_BROWSER

/**
 * The configuration for [Auth]
 */
actual class AuthConfig: AuthConfigDefaults() {

    /**
     * Whether to disable automatic URL checking for PKCE codes, error codes, and session tokens.
     */
    var disableUrlChecking: Boolean = false

    /**
     * Interface to access browser properties like the current hash. By default, null on NodeJS. Can be changed for testing.
     */
    @SupabaseInternal
    var browserBridge: BrowserBridge? = if(IS_BROWSER) BrowserBridgeImpl() else null

}