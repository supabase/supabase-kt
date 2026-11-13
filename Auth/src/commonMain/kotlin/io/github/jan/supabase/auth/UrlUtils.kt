package io.github.jan.supabase.auth

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.url.UrlValidationResult
import io.github.jan.supabase.auth.url.validateHash
import io.github.jan.supabase.auth.user.UserSession
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
        importSession(newSession, source = SessionSource.External)
        onFinish(newSession)
    }
}

@SupabaseInternal
fun HttpRequestBuilder.redirectTo(url: String) {
    this.url.parameters["redirect_to"] = url
}