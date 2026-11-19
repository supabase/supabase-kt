package io.supabase.auth

import io.supabase.auth.server.createServer
import io.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

internal actual suspend fun Auth.startExternalAuth(
    redirectUrl: String?,
    getUrl: suspend (redirectTo: String?) -> String,
    onSessionSuccess: suspend (UserSession) -> Unit
) {
    withContext(Dispatchers.IO) {
        if(redirectUrl != null) {
            config.urlLauncher.openUrl(supabaseClient, getUrl(redirectUrl))
            return@withContext
        }
        createServer({
            getUrl(it)
        }, supabaseClient.auth, onSessionSuccess)
    }
}