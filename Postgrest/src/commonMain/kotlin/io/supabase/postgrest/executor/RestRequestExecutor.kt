package io.supabase.postgrest.executor

import io.supabase.exceptions.HttpRequestException
import io.supabase.postgrest.Postgrest
import io.supabase.postgrest.PostgrestImpl
import io.supabase.postgrest.exception.PostgrestRestException
import io.supabase.postgrest.query.AcceptHeader
import io.supabase.postgrest.query.PostgrestRequestBuilder
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@PublishedApi
internal data object RestRequestExecutor : RequestExecutor {

    private val RETRYABLE_METHODS = setOf(HttpMethod.Get, HttpMethod.Head)
    private const val HTTP_SERVICE_UNAVAILABLE = 503
    private const val HTTP_UNKNOWN_ERROR = 520
    private val RETRYABLE_STATUS_CODES = setOf(HTTP_SERVICE_UNAVAILABLE, HTTP_UNKNOWN_ERROR)
    private const val MAX_RETRY_DELAY_MS = 30_000L
    private const val BASE_DELAY_MS = 1000L
    private const val BACKOFF_MULTIPLIER = 2.0
    private const val RETRY_COUNT_HEADER = "x-retry-count"

    override suspend fun execute(
        postgrest: Postgrest,
        path: String,
        request: PostgrestRequestBuilder
    ): PostgrestResult {
        val maxRetries = postgrest.config.maxRetries
        val shouldRetry = request.retry && request.httpMethod in RETRYABLE_METHODS
        val retryCount = if (shouldRetry) maxRetries else 0

        val authenticatedSupabaseApi = (postgrest as PostgrestImpl).api

        var lastException: Exception? = null
        for (attempt in 0..retryCount) {
            try {
                val response = authenticatedSupabaseApi.request(path) {
                    with(request) { apply() }
                    if (attempt > 0) {
                        headers.append(RETRY_COUNT_HEADER, attempt.toString())
                    }
                }
                return response.asPostgrestResult(postgrest)
            } catch (e: PostgrestRestException) {
                lastException = e
                if (attempt < retryCount && e.statusCode in RETRYABLE_STATUS_CODES) {
                    delay(getRetryDelay(attempt))
                } else if(e.code == "PGRST116" && request.acceptHeader is AcceptHeader.Single && (request.acceptHeader as AcceptHeader.Single).maybe) {
                    return PostgrestResult("", e.response.headers, postgrest)
                } else {
                    throw e
                }
            } catch (e: HttpRequestException) {
                lastException = e
                if (attempt < retryCount) {
                    delay(getRetryDelay(attempt))
                } else {
                    throw e
                }
            }
        }
        throw lastException!!
    }

    private fun getRetryDelay(attempt: Int): Duration {
        val exponentialDelay = BASE_DELAY_MS * BACKOFF_MULTIPLIER.pow(attempt).toLong()
        return minOf(exponentialDelay, MAX_RETRY_DELAY_MS).milliseconds
    }

}
