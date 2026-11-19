package io.supabase.auth

/**
 * Represents the scope of a sign-out action.
 *
 * The sign-out scope determines the scope of the sign-out action being performed.
 */
enum class SignOutScope {
    /**
     * Sign-out action applies to all sessions across the entire system.
     */
    GLOBAL,

    /**
     * Sign-out action applies only to the current session.
     */
    LOCAL,

    /**
     * Sign-out action applies to other sessions, excluding the current session.
     */
    OTHERS
}