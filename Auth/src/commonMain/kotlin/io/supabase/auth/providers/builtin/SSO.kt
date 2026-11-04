package io.supabase.auth.providers.builtin

import io.supabase.SupabaseClient
import io.supabase.auth.auth
import io.supabase.auth.providers.AuthProvider
import io.supabase.auth.startExternalAuth
import io.supabase.auth.user.UserSession
import kotlinx.serialization.Serializable

/**
 * Single Sign On (SSO) auth provider for supabase.
 *
 * Check the [docs](https://supabase.com/docs/guides/auth/sso/auth-sso-saml) for more information.
 *
 */
data object SSO: AuthProvider<SSO.Config, Unit> {

    /**
     * The SSO config
     *
     * Use only one of [providerId] or [domain]
     *
     * @param providerId The provider id of the SSO provider
     * @param captchaToken Optional captcha token
     * @param domain The domain of the SSO provider
     */
    data class Config(
        var providerId: String? = null,
        var captchaToken: String? = null,
        var domain: String? = null,
    )

    /**
     * The result of an SSO login
     * @param url The url to redirect to
     */
    @Serializable
    data class Result(
        val url: String
    )

    override suspend fun login(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (Config.() -> Unit)?
    ) = signUp(supabaseClient, onSuccess, redirectUrl, config)

    override suspend fun signUp(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (Config.() -> Unit)?
    ) = loginWithSSO(
        supabaseClient,
        onSuccess,
        redirectUrl,
        config
    )

}

internal suspend fun loginWithSSO(
    supabaseClient: SupabaseClient,
    onSuccess: suspend (UserSession) -> Unit,
    redirectUrl: String?,
    config: (SSO.Config.() -> Unit)?
) {
    supabaseClient.auth.startExternalAuth(
        redirectUrl = redirectUrl,
        getUrl = {
            supabaseClient.auth.retrieveSSOUrl(it) {
                config?.invoke(this)
            }.url
        },
        onSessionSuccess = onSuccess
    )
}