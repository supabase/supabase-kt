package io.supabase.auth.exception

import io.supabase.auth.SessionManager

/**
 * An exception thrown by a [SessionManager] when there is no session stored.
 *
 * This represents the normal, expected "logged out" state (e.g. a fresh install or after a sign out)
 * and should not be treated as an error.
 *
 * It extends [IllegalStateException] for backwards compatibility, as session managers previously
 * threw [IllegalStateException] in this case.
 */
class NoSessionFoundException : IllegalStateException("No session found in storage")
