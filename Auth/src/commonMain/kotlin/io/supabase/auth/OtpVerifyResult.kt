package io.supabase.auth

import io.supabase.auth.user.UserSession

/**
 * Represents the result for [Auth.verifyEmailOtp] and [Auth.verifyPhoneOtp].
 */
sealed interface OtpVerifyResult {

    /**
     * The OTP was successfully verified and the session was imported.
     * @param session The session received. Handled automatically.
     */
    data class Authenticated(val session: UserSession) : OtpVerifyResult

    /**
     * The OTP was successfully verified, but no authentication session was issued.
     *
     * This occurs when the verification confirms an action rather than signing
     * the user in, such as during a secure email change that requires confirmation
     * from both email addresses.
     */
    data object VerifiedNoSession : OtpVerifyResult

}