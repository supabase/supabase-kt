package io.supabase.auth.providers

import io.supabase.auth.Auth
import io.supabase.logging.d
import io.supabase.logging.e
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal actual fun Auth.openUrl(url: NSURL) {
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>()) {
        if(it) logger.d { "Successfully opened provider url in safari" } else logger.e { "Failed to open provider url in safari" }
    }
}