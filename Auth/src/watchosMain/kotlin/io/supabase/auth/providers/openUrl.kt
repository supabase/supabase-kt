package io.supabase.auth.providers

import io.supabase.auth.Auth
import platform.Foundation.NSURL

internal actual fun Auth.openUrl(url: NSURL) {
    error("Can't open url on watchOS")
}