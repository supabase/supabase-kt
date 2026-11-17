package io.supabase.auth

import io.supabase.SupabaseClient
import io.ktor.util.PlatformUtils.IS_BROWSER
import kotlinx.browser.window

internal actual suspend fun SupabaseClient.openExternalUrl(url: String) {
    if(IS_BROWSER) {
        window.location.href = url
    } else {
        //TODO: Unsupported for now
    }
}