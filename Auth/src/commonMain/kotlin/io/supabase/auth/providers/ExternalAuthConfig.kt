package io.supabase.auth.providers

import io.supabase.auth.Auth

/**
 * Configuration for external authentication providers like Google, Twitter, etc.
 */
expect class ExternalAuthConfig(): ExternalAuthConfigDefaults

/**
 * The default values for [ExternalAuthConfig]
 */
open class ExternalAuthConfigDefaults {

    /**
     * The scopes to request from the external provider
     */
    val scopes = mutableListOf<String>()

    /**
     * Additional query parameters to send to the external provider
     */
    val queryParams = mutableMapOf<String, String>()

    /**
     * Automatically open the URL in the browser. Only applies to [Auth.linkIdentity].
     */
    var automaticallyOpenUrl: Boolean = true

}