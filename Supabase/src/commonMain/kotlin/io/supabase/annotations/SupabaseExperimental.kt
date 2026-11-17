package io.supabase.annotations

/**
 * Used to mark experimental APIs
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "This API is experimental and may not be stable yet")
annotation class SupabaseExperimental
