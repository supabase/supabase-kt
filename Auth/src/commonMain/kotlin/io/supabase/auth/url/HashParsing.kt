package io.supabase.auth.url

import io.supabase.StringMasking
import io.supabase.auth.Auth
import io.supabase.auth.parseSessionFromFragment
import io.supabase.buildUrl
import io.supabase.logging.d
import io.ktor.http.URLDecodeException
import io.ktor.http.parseQueryString

internal fun Auth.validateHash(hash: String): UrlValidationResult {
    logger.d { "Parsing fragment/hash ${StringMasking.maskParameterString(hash)}" }
    val parameters = getFragmentParts(hash)
    if(handledUrlParameterError { parameters[it] }) {
        return UrlValidationResult.ErrorFound
    }
    val session = try {
        parseSessionFromFragment(hash)
    } catch(e: IllegalArgumentException) {
        logger.d(e) { "Received invalid session fragment. Ignoring." }
        return UrlValidationResult.Skipped
    }
    return UrlValidationResult.SessionFound(session)
}

/**
 * Splits a URL fragment into its parameters.
 *
 * The fragment is expected as it appears in the URL, so keys and values are decoded like query parameters:
 * percent escapes are resolved and a plus sign becomes a space. A fragment that cannot be decoded is split as is.
 */
internal fun getFragmentParts(fragment: String): Map<String, String> {
    if(fragment.isEmpty()) return emptyMap()
    val parameters = try {
        parseQueryString(fragment)
    } catch(e: URLDecodeException) {
        parseQueryString(fragment, decode = false)
    }
    return parameters.entries().associate { (key, values) ->
        key to (values.firstOrNull() ?: "")
    }
}

internal fun consumeHashParameters(parameters: List<String>, url: String): String {
    return buildUrl(url) {
        fragment = fragment.split("&").filter {
            it.split("=").first() !in parameters
        }.joinToString("&")
    }
}
