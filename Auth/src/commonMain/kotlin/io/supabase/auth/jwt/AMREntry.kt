@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")

package io.supabase.auth.jwt

import io.supabase.auth.mfa.MfaApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant


/**
 * An authentication method reference (AMR) entry.
 *
 * An entry designates what method was used by the user to verify their
 * identity and at what time.
 *
 * Note: Custom access token hooks can return AMR claims as either:
 * - An array of AMREntry objects (detailed format with timestamps)
 * - An array of strings (RFC-8176 compliant format)
 *
 * @param method Authentication method name.
 * @param timestamp Timestamp when the method was successfully used.
 *
 * @see [MfaApi.getAuthenticatorAssuranceLevel].
 */
@Serializable
data class AMREntry(
    val method: AMRMethod,
    @SerialName("timestamp") private val timestampInt: Long
) {

    val timestamp by lazy { Instant.fromEpochSeconds(timestampInt) }

}

/**
 * Represents an AMR Method.
 * @param value The string name of this method
 */
@Serializable
enum class AMRMethod(val value: String) {
    @SerialName("password") Password("password"),
    @SerialName("otp") OTP("otp"),
    @SerialName("oauth") OAuth("oauth"),
    @SerialName("totp") TOTP("totp"),
    @SerialName("mfa/totp") MFA_TOTP("mfa/totp"),
    @SerialName("mfa/phone") MFA_PHONE("mfa/phone"),
    @SerialName("mfa/webauthn") MFA_WEBAUTHN("mfa/webauthn"),
    @SerialName("anonymous") ANONYMOUS("anonymous"),
    @SerialName("sso/saml") SSO_SAML("sso/saml"),
    @SerialName("magiclink") MAGIC_LINK("magiclink"),
    @SerialName("web3") WEB3("web3")
}