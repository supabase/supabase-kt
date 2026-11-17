package io.supabase.auth

import io.supabase.StringMasking
import io.supabase.auth.url.getFragmentParts
import io.supabase.auth.user.UserSession
import io.supabase.encodeToJsonElement
import io.supabase.logging.d
import kotlinx.serialization.json.jsonObject

internal fun noDeeplinkError(arg: String): Nothing = error("""
        Trying to use a deeplink as a redirect url, but no deeplink $arg is set in the AuthConfig.
        If you want to use deep linking, set the scheme and host in the AuthConfig:
        install(Auth) {
            scheme = "YOUR_SCHEME"
            host = "YOUR_HOST"
        }
        You can also provide a custom redirect url.
    """.trimIndent())

/**
 * Parses a session from a fragment.
 * @param fragment The fragment to parse the session from, as it appears in the URL. Its values are decoded like query parameters, so pass it percent-encoded and not decoded already.
 * @return The parsed session. Note that the user will be null, but you can retrieve it using [Auth.retrieveUser]
 */
fun Auth.parseSessionFromFragment(fragment: String): UserSession {
    val sessionParts = getFragmentParts(fragment)

    logger.d { "Fragment parts: ${StringMasking.maskParameters(sessionParts)}" }

    val accessToken = sessionParts["access_token"] ?: invalidArg("No access token found")
    val refreshToken = sessionParts["refresh_token"] ?: invalidArg("No refresh token found")
    val expiresIn = sessionParts["expires_in"]?.toLong() ?: invalidArg("No expires in found")
    val tokenType = sessionParts["token_type"] ?: invalidArg("No token type found")
    val type = sessionParts["type"] ?: ""
    val providerToken = sessionParts["provider_token"]
    val providerRefreshToken = sessionParts["provider_refresh_token"]

    return UserSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        providerRefreshToken = providerRefreshToken,
        providerToken = providerToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
        user = null,
        type = type
    )
}

/**
 * Parses a session from an url.
 * @param url The url to parse the session from
 * @return The parsed session. Note that the user will be null, but you can retrieve it using [Auth.retrieveUser]
 */
fun Auth.parseSessionFromUrl(url: String): UserSession = parseSessionFromFragment(url.substringAfter("#"))

/**
 * Signs in the user without any credentials. This will create a new user session with a new access token.
 *
 * If you want to upgrade this anonymous user to a real user, use [Auth.linkIdentity] to link an OAuth identity or [Auth.updateUser] to add an email or phone.
 *
 * @param data Extra data for the user
 * @param captchaToken The captcha token to use
 */
suspend inline fun <reified T : Any> Auth.signInAnonymously(data: T, captchaToken: String? = null) = signInAnonymously(serializer.encodeToJsonElement(data).jsonObject, captchaToken)