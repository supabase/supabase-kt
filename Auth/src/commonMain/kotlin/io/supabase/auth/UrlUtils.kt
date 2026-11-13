package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.status.SessionSource
import io.supabase.auth.url.UrlValidationResult
import io.supabase.auth.url.validateHash
import io.supabase.auth.user.UserSession
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.launch

@SupabaseInternal
fun Auth.parseFragmentAndImportSession(fragment: String, onFinish: (UserSession?) -> Unit = {}) {
    val res = validateHash(fragment)
    if(res !is UrlValidationResult.SessionFound) {
        onFinish(null)
        return
    }
    val session = res.session
    authScope.launch {
        val user = tryToGetUser(session.accessToken)
        val newSession = session.copy(user = user)
        onFinish(newSession)
        importSession(newSession, source = SessionSource.External)
    }
}

@SupabaseInternal
fun HttpRequestBuilder.redirectTo(url: String) {
    this.url.parameters["redirect_to"] = url
}