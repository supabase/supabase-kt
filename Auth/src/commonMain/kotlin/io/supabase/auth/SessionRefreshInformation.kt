package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import kotlin.time.Instant

@SupabaseInternal
data class SessionRefreshInformation(
    val autoRefreshStartedAt: Instant,
    val lastRefreshedAt: Instant?,
    val refreshingAt: Instant?
)
