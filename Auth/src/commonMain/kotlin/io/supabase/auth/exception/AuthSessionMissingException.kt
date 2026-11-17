package io.supabase.auth.exception

import io.ktor.client.statement.HttpResponse

/**
 * Exception thrown when a session is not found.
 */
class AuthSessionMissingException(response: HttpResponse): AuthRestException(
    errorCode = CODE,
    response = response,
    errorDescription = "Session not found. This can happen if the user was logged out or deleted."
) {

    internal companion object {
        const val CODE = "session_not_found"
    }

}