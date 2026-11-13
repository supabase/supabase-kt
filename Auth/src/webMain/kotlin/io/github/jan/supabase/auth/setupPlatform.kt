package io.github.jan.supabase.auth

import io.github.jan.supabase.StringMasking
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.url.UrlValidationResult
import io.github.jan.supabase.auth.url.consumeHashParameters
import io.github.jan.supabase.auth.url.consumeUrlParameter
import io.github.jan.supabase.auth.url.handledUrlParameterError
import io.github.jan.supabase.auth.url.validateHash
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.w
import io.ktor.util.PlatformUtils.IS_BROWSER
import org.w3c.dom.url.URL

private fun cleanQueryParams(bridge: BrowserBridge) {
    val newURL = consumeUrlParameter(Auth.QUERY_PARAMETERS, bridge.href)
    bridge.replaceCurrentUrl(newURL);
}

private fun cleanHash(bridge: BrowserBridge) {
    val newURL = consumeHashParameters(Auth.HASH_PARAMETERS, bridge.href)
    bridge.replaceCurrentUrl(newURL)
}

private fun Auth.checkForHash(bridge: BrowserBridge): UserSession? {
    if(bridge.hash.isBlank()) return null
    val afterHash = bridge.hash.substring(1)
    if(!afterHash.contains('=')) return null
    logger.d { "Found hash: ${StringMasking.maskParameterString(afterHash)}" }
    return when(val result = validateHash(afterHash)) {
        is UrlValidationResult.SessionFound, UrlValidationResult.ErrorFound -> {
            cleanHash(bridge)
            (result as? UrlValidationResult.SessionFound)?.session
        }
        UrlValidationResult.Skipped -> null
    }
}

private fun Auth.checkForQueryError(bridge: BrowserBridge): Boolean {
    val url = URL(bridge.href)
    if (!handledUrlParameterError { url.searchParams.get(it) }) return false
    cleanQueryParams(bridge)
    return true
}

private fun Auth.checkForPKCECode(bridge: BrowserBridge): String? {
    val url = URL(bridge.href)
    if (handledUrlParameterError { url.searchParams.get(it) }) {
        cleanQueryParams(bridge)
        return null
    }
    val code = url.searchParams.get("code") ?: return null
    cleanQueryParams(bridge)
    return code
}

private suspend fun Auth.handlePKCECode(code: String) {
    logger.d { "Found PCKE code: ${StringMasking.maskString(code, showLength = true)}" }
    try {
        val session = exchangeCodeForSession(code)
        importSession(session, source = SessionSource.External)
    } catch(e: Exception) {
        logger.w(e) { "Failed to exchange PCKE code for session" }
    }
    initDone()
}

private suspend fun Auth.handleHashSession(session: UserSession) {
    val user = tryToGetUser(session.accessToken)
    val newSession = session.copy(user = user)
    importSession(newSession, source = SessionSource.External)
}

@SupabaseInternal
actual suspend fun Auth.setupPlatform() {
    if(IS_BROWSER && !config.disableUrlChecking && config.browserBridge != null) {
        when(config.flowType) {
            FlowType.PKCE -> {
                logger.d { "Using PCKE flow type, checking for PCKE code..." }
                val code = checkForPKCECode(config.browserBridge!!) ?: return initDone()
                handlePKCECode(code)
            }
            FlowType.IMPLICIT -> {
                logger.d { "Using IMPLICIT flow type, checking for hash..." }
                if (checkForQueryError(config.browserBridge!!)) return initDone()
                val sessionInHash = checkForHash(config.browserBridge!!) ?: return initDone()
                handleHashSession(sessionInHash)
            }
        }
    } else {
        initDone()
    }
}