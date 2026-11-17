package io.supabase.auth

import io.supabase.annotations.SupabaseInternal

@SupabaseInternal
actual suspend fun Auth.setupPlatform() = initDone()