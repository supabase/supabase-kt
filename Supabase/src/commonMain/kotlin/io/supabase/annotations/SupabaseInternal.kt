package io.supabase.annotations

/**
 * Used to mark internal APIs
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "This API is internal and can change at any time")
annotation class SupabaseInternal
