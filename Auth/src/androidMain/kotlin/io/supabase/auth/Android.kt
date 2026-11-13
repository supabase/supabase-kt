package io.supabase.auth

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.url.handledUrlParameterError
import io.supabase.auth.user.UserSession
import kotlinx.coroutines.launch

internal fun openUrl(uri: Uri, action: ExternalAuthAction) {
    when(action) {
        ExternalAuthAction.ExternalBrowser -> {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            applicationContext().startActivity(browserIntent)
        }
        is ExternalAuthAction.CustomTabs -> {
            val intent = CustomTabsIntent.Builder().apply(action.intentBuilder).build()
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.intent.apply(action.intentModifier)
            intent.launchUrl(applicationContext(), uri)
        }
    }
}

//TODO: Add context receiver 'Activity'
/**
 * Handle deeplinks for authentication.
 * This handles the deeplinks for implicit and PKCE flow.
 * @param intent The intent from the activity
 * @param onSessionSuccess The callback when the session was successfully imported
 * @param onError Callback invoked if an error occurs during the [Auth.exchangeCodeForSession] call.
 */
@OptIn(SupabaseInternal::class)
fun SupabaseClient.handleDeeplinks(
    intent: Intent,
    onSessionSuccess: (UserSession) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val data = intent.data ?: return
    val scheme = data.scheme ?: return
    val host = data.host ?: return
    if(scheme != auth.config.scheme || host != auth.config.host) return
    when(this.auth.config.flowType) {
        FlowType.IMPLICIT -> {
            if(auth.handledUrlParameterError { data.getQueryParameter(it) }) {
                return
            }
            val fragment = data.encodedFragment ?: return
            auth.parseFragmentAndImportSession(fragment) {
                it?.let(onSessionSuccess)
            }
        }
        FlowType.PKCE -> {
            if(auth.handledUrlParameterError { data.getQueryParameter(it) }) {
                return
            }
            val code = data.getQueryParameter("code") ?: return
            (auth as AuthImpl).authScope.launch {
                try {
                    this@handleDeeplinks.auth.exchangeCodeForSession(code)
                    onSessionSuccess(this@handleDeeplinks.auth.currentSessionOrNull() ?: error("No session available"))
                } catch (e: Throwable) {
                    onError(e)
                }
            }
        }
    }
}