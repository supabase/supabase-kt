package io.supabase.auth.mfa

import io.supabase.auth.jwt.AMREntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The assurance level of a session
 */
@Serializable
enum class AuthenticatorAssuranceLevel {
    /**
     * The user is logged in using a provider (Password, OAuth, etc.)
     */
    @SerialName("aal1")
    AAL1,

    /**
     * The user is logged in using at least one MFA factor
     */
    @SerialName("aal2")
    AAL2;

    companion object {

        /**
         * Creates an [AuthenticatorAssuranceLevel] from a string
         * @param value The string to create the [AuthenticatorAssuranceLevel] from (either "aal1" or "aal2")
         */
        fun from(value: String): AuthenticatorAssuranceLevel {
            return when (value) {
                "aal1" -> AAL1
                "aal2" -> AAL2
                else -> throw IllegalArgumentException("Unknown AuthenticatorAssuranceLevel: $value")
            }
        }

    }
}

/**
 * @param current The current assurance level of the session
 * @param next The next possible assurance level (for the next login)
 * @param currentAuthenticationMethods A list of current AMR entries
 */
data class MfaLevel(
    val current: AuthenticatorAssuranceLevel,
    val next: AuthenticatorAssuranceLevel,
    val currentAuthenticationMethods: List<AMREntry>
)