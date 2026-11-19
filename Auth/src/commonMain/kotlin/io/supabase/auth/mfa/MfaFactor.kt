package io.supabase.auth.mfa

/**
 * Represents an enrolled MFA Factor
 * @param id The ID of the factor
 * @param type The type of the factor
 * @param data Additional data of the factor (like QR-Code for TOTP)
 */
data class MfaFactor<T>(
    val id: String,
    val type: String,
    val data: T
)