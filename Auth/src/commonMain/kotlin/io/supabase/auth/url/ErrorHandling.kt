package io.supabase.auth.url

import io.supabase.auth.Auth
import io.supabase.auth.event.AuthEvent
import io.supabase.auth.status.SessionStatus
import io.supabase.logging.d

internal fun checkForUrlParameterError(parameters: (String) -> String?): AuthEvent.OtpError? {
    val error = parameters("error")
    val errorCode = parameters("error_code")
    val errorDescription = parameters("error_description")
    val code = errorCode ?: error ?: errorDescription ?: return null
    val description = if(errorDescription != null && error != null) {
        "$errorDescription ($error)"
    } else {
        errorDescription ?: error ?: code
    }
    return AuthEvent.OtpError(
        error = code,
        errorDescription = description,
    )
}

internal fun Auth.handledUrlParameterError(parameters: (String) -> String?): Boolean {
    val error = checkForUrlParameterError(parameters)
    return if(error != null) {
        if(sessionStatus.value !is SessionStatus.Authenticated) {
            logger.d { "Found error code in the URL Parameters: $error. Emitting event..." }
            emitEvent(error)
        }
        true
    } else false
}