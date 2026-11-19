package io.supabase.auth

import io.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

internal actual suspend fun SupabaseClient.openExternalUrl(url: String) {
    withContext(Dispatchers.IO) {
        Desktop.getDesktop()
            .browse(URI(url))
    }
}
