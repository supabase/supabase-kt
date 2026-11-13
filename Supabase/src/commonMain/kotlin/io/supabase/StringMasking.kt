package io.supabase

import io.supabase.annotations.SupabaseInternal
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.util.toMap

@SupabaseInternal
object StringMasking {
    fun maskString(value: String, visibleCharacters: Int = 2, showLength: Boolean = false): String {
        if(value.isBlank()) return value;
        return value.take(visibleCharacters) + "..." + if(showLength) " (len=${value.length})" else ""
    }

    fun maskUrl(value: Url, visibleCharacters: Int = 2): String {
        return buildUrl(value) {
            host = "${host.take(visibleCharacters)}..."
        }
    }

    /**
     * Header field names are case-insensitive per RFC 9110 section 5.1, so the denylist is matched
     * against the lowercased name. Matching the raw name would let `authorization` or `APIKey` —
     * both valid spellings, and both producible by a user supplied `httpConfig { }` block — bypass
     * masking entirely and land a bearer token in an exception message.
     */
    private val SENSITIVE_HEADERS = setOf(
        "apikey",
        "authorization",
        "cookie",
        "set-cookie",
        "proxy-authorization",
        "x-api-key",
    )

    private const val BEARER_PREFIX = "Bearer "

    fun maskHeaders(headers: Headers): String = headers.toMap().mapValues { (key, value) ->
        if(key.lowercase() in SENSITIVE_HEADERS) {
            value.map { headerValue ->
                if(headerValue.startsWith(BEARER_PREFIX)) {
                    "$BEARER_PREFIX${maskString(headerValue.removePrefix(BEARER_PREFIX), showLength = true)}"
                } else {
                    maskString(headerValue, showLength = true)
                }
            }
        } else value
    }.toString()

    /**
     * Parameter names whose values are bearer credentials and must never be logged.
     *
     * These arrive in OAuth redirect fragments and query strings (implicit flow) and in PKCE
     * callbacks, so any log line that renders a raw fragment or a parsed parameter map would
     * otherwise leak a session.
     */
    private val SENSITIVE_PARAMETERS = setOf(
        "access_token",
        "refresh_token",
        "provider_token",
        "provider_refresh_token",
        "id_token",
        "code",
        "code_verifier",
    )

    /**
     * Masks the values of any credential-bearing entries in an already parsed parameter map.
     */
    fun maskParameters(parameters: Map<String, String>): String = parameters.mapValues { (key, value) ->
        if(key.lowercase() in SENSITIVE_PARAMETERS) maskString(value, showLength = true) else value
    }.toString()

    /**
     * Masks the values of any credential-bearing entries in a raw `a=b&c=d` fragment or query
     * string, preserving the original ordering and any malformed segments so the result stays
     * useful for debugging.
     */
    fun maskParameterString(raw: String): String = raw.split("&").joinToString("&") { pair ->
        val separator = pair.indexOf('=')
        if(separator <= 0) return@joinToString pair
        val key = pair.substring(0, separator)
        val value = pair.substring(separator + 1)
        if(key.lowercase() in SENSITIVE_PARAMETERS) "$key=${maskString(value, showLength = true)}" else pair
    }
}
