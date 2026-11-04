package io.supabase.auth

/**
 * OTPs (One Time Passwords) are used to authenticate users via email or phone.
 *
 * An OTP type can be either [Email] or [Phone]
 */
sealed interface OtpType {

    /**
     * The type of the OTP
     */
    val type: String

    /**
     * Email OTP types
     */
    enum class Email(override val type: String): OtpType {
        /**
         * Magic link OTP type
         *
         * **Note: If you verify an OTP using [Auth.verifyEmailOtp] you can use [Email.EMAIL] to cover both the [MAGIC_LINK] and [SIGNUP] OTP types**
         *
         * if you want to resend an OTP, use the according type. In this case [MAGIC_LINK]
         */
        MAGIC_LINK("magiclink"),
        /**
         * OTP type for signing up
         *
         * **Note: If you verify an OTP using [Auth.verifyEmailOtp] you can use [Email.EMAIL] to cover both the [MAGIC_LINK] and [SIGNUP] OTP types**
         *
         * if you want to resend an OTP, use the according type. In this case [SIGNUP]
         */
        SIGNUP("signup"),
        /**
         * OTP type for inviting users
         */
        INVITE("invite"),
        /**
         * OTP type for recovering accounts
         */
        RECOVERY("recovery"),
        /**
         * OTP type for changing email addresses
         */
        EMAIL_CHANGE("email_change"),
        /**
         * OTP type combining all email OTP types
         */
        EMAIL("email")
    }

    /**
     * Phone OTP types
     */
    enum class Phone(override val type: String): OtpType {
        /**
         * OTP type for sms messages
         */
        SMS("sms"),

        /**
         * OTP type for phone change flows
         */
        PHONE_CHANGE("phone_change")
    }

}