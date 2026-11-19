package io.supabase.exceptions

import io.supabase.StringMasking.maskHeaders
import io.supabase.StringMasking.maskUrl
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request

/**
 * Base class for all response-related exceptions
 *
 * Plugins may extend this class to provide more specific exceptions
 * @param error The error returned by Supabase
 * @param description The error description returned by Supabase
 * @param response The response that caused the exception
 * @see UnauthorizedRestException
 * @see BadRequestRestException
 * @see NotFoundRestException
 * @see UnknownRestException
 */
open class RestException(val error: String, val description: String?, val response: HttpResponse):
    Exception(
        error
                + (description?.let { "\n$it" } ?: "")
                + "\nURL: ${maskUrl(response.request.url)}"
                + "\nHeaders: ${maskHeaders(response.request.headers)}"
                + "\nHttp Method: ${response.request.method.value}"
    ) {

    /**
     * The status code of the response
     */
    open val statusCode = response.status.value

}

/**
 * Thrown when supabase-kt receives a response indicating an authentication error
 */
class UnauthorizedRestException(error: String, response: HttpResponse, message: String? = null): RestException(error,  message, response)

/**
 * Thrown when supabase-kt receives a response indicating that the request was invalid due to missing or wrong fields
 */
class BadRequestRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, message, response)

/**
 * Thrown when supabase-kt receives a response indicating that the wanted resource was not found
 */
class NotFoundRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, message, response)

/**
 * Thrown for all other response codes
 */
class UnknownRestException(error: String, response: HttpResponse, message: String? = null): RestException(error, message, response)
