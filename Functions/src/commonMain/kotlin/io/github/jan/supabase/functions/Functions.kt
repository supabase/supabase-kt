package io.github.jan.supabase.functions

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthDependentPluginConfig
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.encode
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.logging.createLogger
import io.github.jan.supabase.plugins.CustomSerializationConfig
import io.github.jan.supabase.plugins.CustomSerializationPlugin
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Plugin to interact with the supabase Edge Functions API
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Functions)
 * }
 * ```
 *
 * then you can use it like this:
 * ```kotlin
 * val response = supabase.functions("myFunction")
 * //or store it in a variable
 * val function = supabase.functions.buildEdgeFunction("myFunction")
 * val response = function()
 * ```
 */
class Functions(override val config: Config, override val supabaseClient: SupabaseClient) : MainPlugin<Functions.Config>, CustomSerializationPlugin {

    override val logger: SupabaseLogger = supabaseClient.createLogger(Functions.LOGGING_TAG, config)
    override val apiVersion: Int
        get() = API_VERSION

    override val pluginKey: String
        get() = key

    override val serializer = config.serializer ?: supabaseClient.defaultSerializer

    @OptIn(SupabaseInternal::class)
    @PublishedApi
    internal val api = supabaseClient.authenticatedSupabaseApi(this) {
        useNewApiKeyAsFallback = false
    }

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * @param function The function to invoke. If name of the function is renamed, use the slug after URL
     * @param builder The request builder to configure the request
     * @param region The region where the function is invoked
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend inline operator fun invoke(function: String, region: FunctionRegion = config.defaultRegion, crossinline builder: HttpRequestBuilder.() -> Unit): HttpResponse {
        return api.post(function) {
            builder()
            setRegionHeader(region)
        }
    }

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * Note, if you want to serialize [body] to json, you need to add the [HttpHeaders.ContentType] header yourself.
     * @param function The function to invoke. If name of the function is renamed, use the slug after URL
     * @param body The body of the request
     * @param headers Headers to add to the request
     * @param region The region where the function is invoked
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend inline operator fun <reified T : Any> invoke(function: String, body: T, region: FunctionRegion = config.defaultRegion, headers: Headers = Headers.Empty): HttpResponse = invoke(function, region) {
        this.headers.appendAll(headers)
        setBody(serializer.encode(body))
    }

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * @param function The function to invoke. If name of the function is renamed, use the slug after URL
     * @param headers Headers to add to the request
     * @param region The region where the function is invoked
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend inline operator fun invoke(function: String, region: FunctionRegion = config.defaultRegion, headers: Headers = Headers.Empty): HttpResponse = invoke(function, region) {
        this.headers.appendAll(headers)
    }

    /**
     * Invokes a remote edge function that returns Server-Sent Events (SSE) and returns
     * a [Flow] of [FunctionServerSentEvent]. Each event provides access to [data][FunctionServerSentEvent.data],
     * [event][FunctionServerSentEvent.event], and [id][FunctionServerSentEvent.id] fields, as well as
     * a [decodeAs][FunctionServerSentEvent.decodeAs] method for deserializing the data payload.
     *
     * The authorization token is automatically added to the request.
     * Ktor's SSE plugin requires direct [io.ktor.client.HttpClient] access, so this method uses the raw client
     * with [api.getDefaultHeaders] to include the authentication token (base headers like apikey
     * are already applied by Ktor's DefaultRequest plugin).
     * @param function The function to invoke. If name of the function is renamed, use the slug after URL
     * @param region The region where the function is invoked
     * @param builder The request builder to configure the request
     * @return A [Flow] of [FunctionServerSentEvent] representing the SSE stream
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    @OptIn(SupabaseInternal::class)
    fun invokeSSE(
        function: String,
        region: FunctionRegion = config.defaultRegion,
        builder: HttpRequestBuilder.() -> Unit = {}
    ): Flow<FunctionServerSentEvent> = flow {
        val defaultHeaders = api.getDefaultHeaders()
        val resolvedUrl = resolveUrl(function)
        val ktorClient = supabaseClient.httpClient.httpClient
        ktorClient.sse(
            urlString = resolvedUrl,
            request = {
                method = HttpMethod.Post
                headers.appendAll(defaultHeaders)
                builder()
                setRegionHeader(region)
            }
        ) {
            incoming.collect { event ->
                emit(FunctionServerSentEvent(
                    data = event.data,
                    event = event.event,
                    id = event.id,
                    serializer = this@Functions.serializer
                ))
            }
        }
    }

    /**
     * Prepares an invocation of a remote edge function, returning an [HttpStatement] for streaming
     * or other advanced response handling. The authorization token is automatically added to the request.
     * @param function The function to invoke. If name of the function is renamed, use the slug after URL
     * @param region The region where the function is invoked
     * @param builder The request builder to configure the request
     */
    suspend inline fun prepareInvoke(function: String, region: FunctionRegion = config.defaultRegion, crossinline builder: HttpRequestBuilder.() -> Unit = {}): HttpStatement {
        return api.prepareRequest(function) {
            method = HttpMethod.Post
            builder()
            setRegionHeader(region)
        }
    }

    /**
     * Builds an [EdgeFunction] which can be invoked multiple times
     * @param function The function name. If name of the function is renamed, use the slug after URL
     * @param headers Headers to add to the requests when invoking the function
     * @param region The region where the function is invoked
     */
    @OptIn(SupabaseInternal::class)
    fun buildEdgeFunction(
        function: String,
        region: FunctionRegion = config.defaultRegion,
        headers: Headers = Headers.Empty
    ) = EdgeFunction(
        functionName = function,
        headers = Headers.build {
            appendAll(headers)
            append(X_REGION_HEADER, region.value)
        },
        supabaseClient = supabaseClient
    )

    @PublishedApi
    internal fun HttpRequestBuilder.setRegionHeader(region: FunctionRegion) {
        if (!headers.contains(X_REGION_HEADER)) header(X_REGION_HEADER, region.value)
        if (headers[X_REGION_HEADER] == FunctionRegion.ANY.value) headers.remove(X_REGION_HEADER)
        headers[X_REGION_HEADER]?.let { url.parameters["forceFunctionRegion"] = it }
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        val error = response.bodyAsText()
        return when(response.status) {
            HttpStatusCode.Unauthorized -> UnauthorizedRestException(error, response)
            HttpStatusCode.NotFound -> NotFoundRestException(error, response)
            HttpStatusCode.BadRequest -> BadRequestRestException(error, response)
            else -> UnknownRestException(error, response)
        }
    }

    /**
     * The config for the [Functions] plugin
     * @param customUrl A custom url to use for the requests. If not provided, the default url will be used
     * @param jwtToken A jwt token to use for the requests. If not provided, the token from the [Auth] plugin, or the supabaseKey will be used
     * @property serializer A serializer used for serializing/deserializing objects e.g. in [Functions.invoke] or [EdgeFunction.invoke]. Defaults to [KotlinXSerializer]
     */
    class Config : MainConfig(), CustomSerializationConfig, AuthDependentPluginConfig {

        override var serializer: SupabaseSerializer? = null

        /**
         * The default region to use when invoking a function
         */
        var defaultRegion: FunctionRegion = FunctionRegion.ANY

        override var requireValidSession: Boolean = false

    }

    /**
     * @see Functions
     */
    companion object : SupabasePluginProvider<Config, Functions> {

        override val key = "functions"

        /**
         * The tag for the Functions logger.
         */
        const val LOGGING_TAG = "Supabase-Functions"

        /**
         * The header for specifying which region the function should be called in
         */
        const val X_REGION_HEADER = "x-region"

        /**
         * The current functions api version
         */
        const val API_VERSION = 1

        override fun create(supabaseClient: SupabaseClient, config: Config): Functions {
            return Functions(config, supabaseClient)
        }

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)

        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            builder.httpConfig {
                install(SSE)
            }
        }

    }

}

/**
 * The Functions plugin handles everything related to supabase's edge functions
 */
val SupabaseClient.functions: Functions
    get() = pluginManager.getPlugin(Functions)

