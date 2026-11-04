@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")

package io.supabase.auth.api

import io.supabase.StringMasking
import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.exception.SessionRequiredException
import io.supabase.exceptions.RestException
import io.supabase.network.SupabaseApi
import io.supabase.network.SupabaseHttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.Headers
import io.ktor.http.headers


@OptIn(SupabaseInternal::class)
class AuthenticatedSupabaseApi @SupabaseInternal constructor(
    httpClient: SupabaseHttpClient,
    val config: AuthenticatedApiConfig
): SupabaseApi(config.context.resolveUrl, config.context.parseErrorResponse, httpClient) {

    private val defaultRequest = config.request.defaultRequest
    private val jwtToken = config.auth.jwtToken
    private val requireSession = config.auth.requireSession

    private fun String?.checkIsNewApiKey() = if(this != null && (config.auth.useNewApiKeyAsFallback || !SupabaseClient.checkIsNewApiKey(this))) this else null

    override suspend fun getDefaultHeaders(): Headers {
        val clientHeaders = super.getDefaultHeaders()
        val accessToken = config.auth.getAccessToken.resolve(jwtToken, !requireSession).checkIsNewApiKey()
            ?: throwIfRequired("")
        return headers {
            appendAll(clientHeaders)
            accessToken?.let {
                set("Authorization", "Bearer $it")
            }
        }
    }

    override suspend fun rawRequest(url: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse {
        val accessToken = config.auth.getAccessToken.resolve(jwtToken, !requireSession).checkIsNewApiKey()
            ?: throwIfRequired(url)
        return super.rawRequest(url) {
            accessToken?.let { bearerAuth(it) }
            defaultRequest?.invoke(this)
            builder()
            checkUrlLength()
        }
    }

    private fun throwIfRequired(url: String) = if(config.auth.requireSession) throw SessionRequiredException(url) else null

    suspend fun rawRequest(builder: HttpRequestBuilder.() -> Unit): HttpResponse = rawRequest("", builder)

    override suspend fun prepareRequest(
        url: String,
        builder: HttpRequestBuilder.() -> Unit
    ): HttpStatement {
        val accessToken = config.auth.getAccessToken.resolve(jwtToken, !requireSession).checkIsNewApiKey()
            ?: throwIfRequired(url)
        return super.prepareRequest(url) {
            accessToken?.let { bearerAuth(it) }
            builder()
            defaultRequest?.invoke(this)
            checkUrlLength()
        }
    }

    private fun HttpRequestBuilder.checkUrlLength() {
        if(config.request.urlLengthLimit == null) return
        val length = this.url.toString().length
        if(length > config.request.urlLengthLimit) error("Your URL length exceeds the limit of ${config.request.urlLengthLimit} characters ($length). Url: ${StringMasking.maskUrl(this.url.build())}")
    }

    fun withDefaultRequest(builder: HttpRequestBuilder.() -> Unit): AuthenticatedSupabaseApi =
        AuthenticatedSupabaseApi(
            httpClient = this.httpClient,
            config = this.config.copy(request = this.config.request.copy(defaultRequest = {
                this@AuthenticatedSupabaseApi.config.request.defaultRequest?.invoke(this)
                builder()
            }))
        )

    fun resolve(path: String): AuthenticatedSupabaseApi = AuthenticatedSupabaseApi(
        httpClient = this.httpClient,
        config = this.config.copy(context = this.config.context.copy(resolveUrl = { this.resolveUrl("$path/$it") }))
    )

    companion object {

        @SupabaseInternal
        val DEFAULT_MOCK_URL get() = "https://supabase.com"

        /** [AuthenticatedSupabaseApi] for mocking purposes */
        @SupabaseInternal
        fun minimalAuthenticatedApi(
            httpClient: SupabaseHttpClient,
            resolveUrl: (path: String) -> String = { "${DEFAULT_MOCK_URL}/$it" },
            parseErrorResponse: (suspend (response: HttpResponse) -> RestException)? = null,
            config: AuthenticatedApiConfig = AuthenticatedApiConfig.Builder().apply {
                this.resolveUrl = resolveUrl
                this.parseErrorResponse = parseErrorResponse
                this.requireSession = false
                this.getAccessToken = ResolveAccessToken { token, _ -> token }
                this.jwtToken = "accessToken"
            }.build()
        ) = AuthenticatedSupabaseApi(config = config, httpClient = httpClient)
    }

}