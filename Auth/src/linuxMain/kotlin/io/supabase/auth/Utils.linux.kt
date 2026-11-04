package io.supabase.auth

import io.supabase.SupabaseClient
import platform.posix.system

internal actual suspend fun SupabaseClient.openExternalUrl(url: String) {
    system("xdg-open $url")
}