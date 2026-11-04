package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_BROWSER
import kotlinx.browser.window

@SupabaseInternal
actual fun Auth.defaultPlatformRedirectUrl(): String? = if(IS_BROWSER) window.location.origin else null