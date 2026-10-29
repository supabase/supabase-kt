package io.github.jan.supabase.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.StringMasking
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.logging.e
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal fun invalidArg(message: String): Nothing = throw IllegalArgumentException(message)

internal expect suspend fun SupabaseClient.openExternalUrl(url: String)

internal expect suspend fun Auth.startExternalAuth(
    redirectUrl: String?,
    getUrl: suspend (redirectTo: String?) -> String,
    onSessionSuccess: suspend (UserSession) -> Unit
)

internal fun Auth.initDone() {
    if(sessionStatus.value is SessionStatus.Initializing) {
        setSessionStatus(SessionStatus.NotAuthenticated())
    }
}

internal fun Auth.resetSessionStatusForBackground() {
    if(config.resetStatusOnBackground) {
        setSessionStatus(SessionStatus.Initializing)
    }
}

internal suspend fun Auth.tryToGetUser(accessToken: String) = try {
    retrieveUser(accessToken)
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    logger.e(e) { "Couldn't retrieve user using access token ${StringMasking.maskString(accessToken, showLength = true)}.\nIf you use the project secret, ignore this message." }
    null
}