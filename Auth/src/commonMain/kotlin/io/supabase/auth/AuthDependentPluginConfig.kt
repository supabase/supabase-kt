package io.supabase.auth

import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseExperimental
import io.supabase.auth.user.UserSession

/**
 * Configuration for plugins depending on the Auth plugin
 */
interface AuthDependentPluginConfig {

    /**
     * Whether to require a valid [UserSession] in the [Auth] plugin to make any request with this plugin. The [SupabaseClient.supabaseKey] cannot be used as fallback.
     */
    @SupabaseExperimental
    var requireValidSession: Boolean

}
