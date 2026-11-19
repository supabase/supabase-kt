package io.supabase.storage

import io.supabase.exceptions.RestException
import io.ktor.client.statement.HttpResponse

/**
 * Represents an exception thrown if the storage API returns an error response.
 * @param error A short error identifier string returned by the API, if available
 * @param description A human-readable description of the error.
 * @param statusCode The HTTP status code returned by the API
 * @param code Service-specific error code from the Storage API response body, such as
 * `NoSuchKey`, `AccessDenied` or `ResourceAlreadyExists`. Use this to branch
 * on the specific error rather than parsing the message. Falls back to [error] when the
 * server does not send a code.
 * See https://supabase.com/docs/guides/storage/debugging/error-codes for a list of codes.
 */
class StorageRestException(
    error: String,
    description: String,
    response: HttpResponse,
    override val statusCode: Int,
    val code: String
): RestException(error, description, response)