@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")
package io.supabase.auth.user

import io.supabase.StringMasking
import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Serializable
data class UserSession(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("provider_refresh_token")
    val providerRefreshToken: String? = null,
    @SerialName("provider_token")
    val providerToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("token_type")
    val tokenType: String,
    val user: UserInfo? = null,
    @SerialName("type")
    val type: String = "",
    val expiresAt: Instant = Clock.System.now() + (expiresIn.seconds),
) {

    /**
     * Renders all fields of this session, applying [renderToken] to every token value.
     */
    internal fun render(renderToken: (String) -> String): String = buildString {
        append("UserSession(")
        append("accessToken=").append(renderToken(accessToken))
        append(", refreshToken=").append(renderToken(refreshToken))
        append(", providerRefreshToken=").append(providerRefreshToken?.let(renderToken))
        append(", providerToken=").append(providerToken?.let(renderToken))
        append(", expiresIn=").append(expiresIn)
        append(", tokenType=").append(tokenType)
        append(", user=").append(user)
        append(", type=").append(type)
        append(", expiresAt=").append(expiresAt)
        append(')')
    }

    /**
     * Renders this session with [accessToken], [refreshToken], [providerToken] and
     * [providerRefreshToken] masked. Use [unsafeToString] for the raw values.
     */
    @OptIn(SupabaseInternal::class)
    override fun toString(): String = render { StringMasking.maskString(it, showLength = true) }

}

/**
 * Renders this session with the raw, unmasked token values. Intended for debugging only, never log
 * the result.
 */
@SupabaseInternal
fun UserSession.unsafeToString(): String = render { it }
