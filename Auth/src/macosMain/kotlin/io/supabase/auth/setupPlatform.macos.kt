package io.supabase.auth

// macOS does not suspend processes the way iOS does, so the delay-based
// auto-refresh timer keeps running and no lifecycle handling is needed.
internal actual suspend fun Auth.setupApplePlatform() = initDone()
