package io.supabase.storage.resumable

import io.supabase.annotations.SupabaseInternal

@SupabaseInternal
actual fun createDefaultResumableCache(): ResumableCache = MemoryResumableCache()