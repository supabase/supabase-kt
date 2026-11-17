package io.supabase.postgrest.exception

import io.supabase.exceptions.RestException
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonElement

/**
 * Exception thrown when a Postgrest request fails
 * @param message The error message
 * @param hint A hint to the error
 * @param details Additional details about the error
 * @param code The error code
 * @param response The response that caused the exception
 */
class PostgrestRestException(
    message: String,
    val hint: String?,
    val details: JsonElement?,
    val code: String?,
    response: HttpResponse
): RestException(message, """
    Code: $code
    Hint: $hint
    Details: $details
""".trimIndent(), response)