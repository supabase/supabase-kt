package io.supabase.auth

import io.supabase.annotations.SupabaseInternal

@SupabaseInternal
actual suspend fun Auth.setupPlatform() = setupApplePlatform()

/**
 * Apple targets differ in how (and whether) the OS suspends the process, so
 * the lifecycle handling is per-family: UIKit targets (iOS, tvOS) register
 * foreground/background observers, the others complete initialization only.
 */
internal expect suspend fun Auth.setupApplePlatform()
