package io.supabase.auth.mfa

/**
 * Represents the MFA status of a user.
 * @property enabled Whether MFA is enabled for the user. If true, the user can log in using MFA and has at least one verified factor.
 * @property active Whether MFA is active for the user. If true, the user is logged in using MFA.
 */
data class MfaStatus(
    val enabled: Boolean,
    val active: Boolean
)
