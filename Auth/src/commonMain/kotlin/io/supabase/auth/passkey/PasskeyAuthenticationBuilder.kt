package io.supabase.auth.passkey

/**
 * Builder for [AuthPasskeyApi.startAuthentication]
 * @param captchaToken An optional captcha token for the authentication
 */
data class PasskeyAuthenticationBuilder(
    var captchaToken: String? = null
)
