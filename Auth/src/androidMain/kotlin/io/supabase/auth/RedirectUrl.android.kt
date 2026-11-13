package io.supabase.auth

import io.supabase.annotations.SupabaseInternal

@SupabaseInternal
internal actual fun Auth.defaultPlatformRedirectUrl(): String? = config.deepLinkOrNull