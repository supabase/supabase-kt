package io.supabase.auth.providers

import io.supabase.SupabaseClient
import io.supabase.auth.auth
import io.supabase.auth.startExternalAuth
import io.supabase.auth.user.UserSession

/**
 * Represents an OAuth provider.
 */
abstract class OAuthProvider : AuthProvider<ExternalAuthConfig, Unit> {

    /**
     * The name of the provider.
     */
    abstract val name: String

    override suspend fun login(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (ExternalAuthConfig.() -> Unit)?
    ) {
        val authConfig = ExternalAuthConfig()
        if (config != null) {
            authConfig.config()
        }
        supabaseClient.auth.startExternalAuth(
            redirectUrl = redirectUrl,
            getUrl = {
                supabaseClient.auth.getOAuthUrl(this, it) {
                    scopes.addAll(authConfig.scopes)
                    queryParams.putAll(authConfig.queryParams)
                }
            },
            onSessionSuccess = onSuccess
        )
    }

    override suspend fun signUp(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (ExternalAuthConfig.() -> Unit)?
    ) = login(supabaseClient, onSuccess, redirectUrl, config)

    companion object

}