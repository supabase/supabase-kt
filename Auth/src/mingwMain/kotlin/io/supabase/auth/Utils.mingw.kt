package io.supabase.auth

import io.supabase.SupabaseClient
import kotlinx.cinterop.ExperimentalForeignApi
import platform.windows.SW_SHOWNORMAL
import platform.windows.ShellExecuteW

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun SupabaseClient.openExternalUrl(url: String) {
    ShellExecuteW(null, "open", url, null, null, SW_SHOWNORMAL);
}