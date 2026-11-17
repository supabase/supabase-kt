package io.supabase.auth.providers

import io.supabase.auth.Auth
import io.supabase.logging.d
import io.supabase.logging.e
import platform.AppKit.NSWorkspace
import platform.AppKit.NSWorkspaceOpenConfiguration
import platform.Foundation.NSURL

internal actual fun Auth.openUrl(url: NSURL) {
    NSWorkspace.sharedWorkspace().openURL(url, NSWorkspaceOpenConfiguration()) { _, error ->
        if(error != null) logger.d { "Successfully opened provider url in safari" } else logger.e { "Failed to open provider url in safari" }
    }
}