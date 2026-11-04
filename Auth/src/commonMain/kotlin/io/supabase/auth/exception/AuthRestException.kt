package io.supabase.auth.exception

import io.supabase.exceptions.RestException
import io.ktor.client.statement.HttpResponse

/**
 * Base class for rest exceptions thrown by the Auth API.
 * @property errorCode The error code of the rest exception. This should be a known [AuthErrorCode]. If it is not, use [error] instead.
 * @param errorDescription The description of the error.
 */
open class AuthRestException(errorCode: String, val errorDescription: String, response: HttpResponse): RestException(
    error = errorCode,
    description = "$errorDescription: $errorCode",
    response = response
) {

    /**
     * The error code of the rest exception. If [errorCode] is not a known [AuthErrorCode], this will be null. Then, use [error] instead to get the raw unknown error code.
     */
    val errorCode: AuthErrorCode? = AuthErrorCode.fromValue(errorCode)

}