package io.supabase.exceptions

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.io.IOException

/**
 * An exception that is thrown when a request fails due to network issues
 */
class HttpRequestException(message: String, request: HttpRequestBuilder): IOException("HTTP request to ${request.url.buildString()} (${request.method.value}) failed with message: $message")