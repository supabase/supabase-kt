package io.supabase.auth.status

import io.supabase.auth.Auth
import io.supabase.auth.user.UserSession

/**
 * Represents the status of the current session in [Auth]
 */
sealed interface SessionStatus {

    /**
     * This status means that the user is not logged in
     * @param isSignOut Whether this status was caused by a sign-out
     */
    data class NotAuthenticated(
        val isSignOut: Boolean = false,
    ) : SessionStatus

    /**
     * This status means that [Auth] is currently initializing the session
     */
    data object Initializing : SessionStatus

    /**
     * This status means the session expired and [Auth] is trying to refresh it
     * @param cause The cause of the error. This property will be removed in a future version. Use the new AuthEvent.RefreshFailure(cause) event to diagnose failures.
     */
    data class RefreshFailure(
        @Deprecated("This property will be removed in a future version. Use the new AuthEvent.RefreshFailure(cause) event to diagnose failures.")
        val cause: RefreshFailureCause
    ) : SessionStatus

    /**
     * This status means that [Auth] holds a valid session
     * @param session The session
     * @param source The source of the session
     */
    data class Authenticated(
        val session: UserSession,
        val source: SessionSource = SessionSource.Unknown
    ) : SessionStatus {

        /**
         * Whether the session is new, i.e. [source] is [SessionSource.SignIn], [SessionSource.SignUp] or [SessionSource.External].
         * Use this to determine whether this status is the result of a new sign in or sign up or just a session refresh.
         */
        val isNew: Boolean = source is SessionSource.SignIn || source is SessionSource.SignUp || source is SessionSource.External

    }

}