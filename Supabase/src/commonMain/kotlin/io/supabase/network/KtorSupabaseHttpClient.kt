@file:Suppress("UndocumentedPublicFunction")
package io.supabase.network

import io.supabase.BuildConfig
import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseInternal
import io.supabase.exceptions.HttpRequestException
import io.supabase.logging.d
import io.supabase.supabaseJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

private const val HTTPS_PORT = 443

/**
 * A function that can be used to override the default request configuration
 */
typealias HttpRequestOverride = HttpRequestBuilder.() -> Unit

/**
 * A [SupabaseHttpClient] that uses ktor to send requests
 */
@OptIn(SupabaseInternal::class)
class KtorSupabaseHttpClient @SupabaseInternal constructor(
    private val supabase: SupabaseClient
): SupabaseHttpClient() {
    
    val logger = supabase.logger.appendTag(" [Network]")
    private val supabaseKey = supabase.supabaseKey
    private val osInformation = supabase.config.osInformation

    private val networkConfig = supabase.config.networkConfig
    private val requestTimeout = networkConfig.requestTimeout
    private val engine = networkConfig.httpEngine
    private val modifiers = networkConfig.httpConfigOverrides

    override suspend fun getDefaultHeaders(): Headers {
        return io.ktor.http.headers {
            defaultHeaders()
        }
    }

    init {
        logger.d { "Creating KtorSupabaseHttpClient with request timeout $requestTimeout, HttpClientEngine: $engine" }
    }

    @SupabaseInternal
    val httpClient =
        if(engine != null) HttpClient(engine) { applyDefaultConfiguration(modifiers) }
        else HttpClient { applyDefaultConfiguration(modifiers) }

    override suspend fun request(url: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse {
        val request = HttpRequestBuilder().apply {
            url(url)
            builder()
        }
        val endPoint = request.url.encodedPath
        logger.d { "Starting ${request.method.value} request to endpoint $endPoint" }
        val response = try {
            httpClient.request(url, builder)
        } catch(e: HttpRequestTimeoutException) {
            logger.d(e) { "${request.method.value} request to endpoint $endPoint timed out after $requestTimeout" }
            throw e
        } catch(e: CancellationException) {
            logger.d(e) { "${request.method.value} request to endpoint $endPoint was cancelled"}
            throw e
        } catch(e: Exception) {
            logger.d(e) { "${request.method.value} request to endpoint $endPoint failed with exception ${e.message}" }
            throw HttpRequestException(e.message ?: "", request)
        }
        val responseTime = (response.responseTime.timestamp - response.requestTime.timestamp).milliseconds
        logger.d { "${request.method.value} request to endpoint $endPoint successfully finished in $responseTime" }
        return response
    }

    override suspend fun prepareRequest(
        url: String,
        builder: HttpRequestBuilder.() -> Unit
    ): HttpStatement {
        val request = HttpRequestBuilder().apply {
            url(url)
            builder()
        }
        val response = try {
            httpClient.prepareRequest(url, builder)
        } catch(e: HttpRequestTimeoutException) {
            logger.d(e) { "Request timed out after $requestTimeout on url $url" }
            throw e
        } catch(e: CancellationException) {
            logger.d(e) { "Request was cancelled on url $url" }
            throw e
        } catch(e: Exception) {
            logger.d(e) { "Request failed with ${e.message} on url $url" }
            throw HttpRequestException(e.message ?: "", request)
        }
        return response
    }

    fun close() = httpClient.close()

    private fun HeadersBuilder.defaultHeaders() {
        if(supabaseKey.isNotBlank()) {
            append("apikey", supabaseKey)
        }
        val metadata = buildMap {
            osInformation?.let { os ->
                put("platform", os.name)
                os.version?.let { version ->
                    put("platform-version", version)
                }
            }
            put("runtime", "kotlin")
            put("runtime-version", KotlinVersion.CURRENT.toString())
        }
        append("X-Client-Info", "supabase-kt/${BuildConfig.PROJECT_VERSION}" + metadata.toList().joinToString("; ", prefix = "; ") { "${it.first}=${it.second}" })
    }

    private fun HttpClientConfig<*>.applyDefaultConfiguration(modifiers: List<HttpClientConfig<*>.() -> Unit>) {
        install(DefaultRequest) {
            headers {
                defaultHeaders()
            }
            port = HTTPS_PORT
        }
        install(ContentNegotiation) {
            json(supabaseJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeout.inWholeMilliseconds
        }
        modifiers.forEach { it.invoke(this) }
    }

}