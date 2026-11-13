package io.supabase.auth.status

import io.supabase.auth.Auth
import io.supabase.auth.providers.AuthProvider
import io.supabase.auth.user.UserSession

/**
 * Represents the source of a session
 */
sealed interface SessionSource {

    /**
     * The session was loaded from storage
     */
    data object Storage : SessionSource

    /**
     * The session was loaded from an anonymous sign in
     */
    data object AnonymousSignIn : SessionSource

    /**
     * The session was loaded from a sign in
     * @param provider The provider that was used to sign in
     */
    data class SignIn(val provider: AuthProvider<*, *>) : SessionSource

    /**
     * The session was loaded from a sign-up (only if auto-confirm is enabled)
     * @param provider The provider that was used to sign up
     */
    data class SignUp(val provider: AuthProvider<*, *>) : SessionSource

    /**
     * The session comes from an external source, e.g. OAuth via deep links.
     */
    data object External : SessionSource

    /**
     * The session comes from an unknown source
     */
    data object Unknown : SessionSource

    /**
     * The session was refreshed
     * @param oldSession The old session
     */
    data class Refresh(val oldSession: UserSession) : SessionSource

    /**
     * The session was changed due to a user change (e.g. via [Auth.updateUser] or [Auth.retrieveUserForCurrentSession])
     * @param oldSession The old session
     */
    data class UserChanged(val oldSession: UserSession) : SessionSource

    /**
     * The session was changed due to a user identity change (e.g. via [Auth.linkIdentity] or [Auth.unlinkIdentity])
     * @param oldSession The old session
     */
    data class UserIdentitiesChanged(val oldSession: UserSession) : SessionSource
}