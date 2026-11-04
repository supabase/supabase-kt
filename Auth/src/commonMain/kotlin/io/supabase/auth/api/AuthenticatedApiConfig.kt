package io.supabase.auth.api

import io.supabase.annotations.SupabaseInternal
import io.supabase.exceptions.RestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse

/**
 * [resolve] returns the current access token to be used in the request
 */
fun interface ResolveAccessToken {

    /**
     * @param jwtToken if present, this token will be used
     * @param fallbackToKey If no access token was found, the api key will be used as fallback
     * @return the access token or null if no token was found and [fallbackToKey] is false
     */
    suspend fun resolve(jwtToken: String?, fallbackToKey: Boolean): String?

}

@SupabaseInternal
data class AuthenticatedApiConfig(
    val context: Context,
    val auth: Auth,
    val request: Request = Request()
) {

    @SupabaseInternal
    data class Context(
        val resolveUrl: (path: String) -> String,
        val parseErrorResponse: (suspend (response: HttpResponse) -> RestException)? = null
    )

    @SupabaseInternal
    data class Auth(
        val requireSession: Boolean,
        val getAccessToken: ResolveAccessToken,
        val jwtToken: String? = null,
        val useNewApiKeyAsFallback: Boolean
    )

    @SupabaseInternal
    data class Request(
        val defaultRequest: (HttpRequestBuilder.() -> Unit)? = null,
        val urlLengthLimit: Int? = null
    )

    @SupabaseInternal
    class Builder {

        var resolveUrl: ((path: String) -> String)? = null
        var parseErrorResponse: (suspend (response: HttpResponse) -> RestException)? = null
        var jwtToken: String? = null
        var defaultRequest: (HttpRequestBuilder.() -> Unit)? = null
        var requireSession: Boolean = false
        var urlLengthLimit: Int? = null
        var getAccessToken: ResolveAccessToken? = null
        var useNewApiKeyAsFallback: Boolean = true

        fun build() = AuthenticatedApiConfig(
            context = Context(
                resolveUrl = resolveUrl ?: { it },
                parseErrorResponse = parseErrorResponse
            ),
            auth = Auth(
                requireSession = requireSession,
                getAccessToken = getAccessToken ?: ResolveAccessToken { token, _ -> token },
                jwtToken = jwtToken,
                useNewApiKeyAsFallback = useNewApiKeyAsFallback
            ),
            request = Request(
                defaultRequest = defaultRequest,
                urlLengthLimit = urlLengthLimit
            )
        )

    }

}

internal inline fun buildAuthConfig(builder: AuthenticatedApiConfig.Builder.() -> Unit): AuthenticatedApiConfig {
    val builder = AuthenticatedApiConfig.Builder().apply(builder)
    return builder.build()
}


internal fun AuthenticatedApiConfig.withContext(
    resolveUrl: (path: String) -> String,
    parseErrorResponse: (suspend (response: HttpResponse) -> RestException)?
): AuthenticatedApiConfig = copy(
    context = context.copy(
        resolveUrl = resolveUrl,
        parseErrorResponse = parseErrorResponse
    )
)