package io.supabase.auth

import io.supabase.annotations.SupabaseInternal

@SupabaseInternal
actual fun Auth.createDefaultSessionManager(): SessionManager = MemorySessionManager()

@SupabaseInternal
actual fun Auth.createDefaultCodeVerifierCache(): CodeVerifierCache = MemoryCodeVerifierCache()