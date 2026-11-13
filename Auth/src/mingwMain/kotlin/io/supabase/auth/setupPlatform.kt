package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import io.supabase.logging.w

@SupabaseInternal
actual suspend fun Auth.setupPlatform() {
    logger.w { "Windows support is experimental, please report any bugs you find!" }
    initDone()
}