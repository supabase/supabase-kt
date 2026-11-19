package io.supabase

/**
 * The current target platform
 */
actual val CurrentPlatformTarget: PlatformTarget = PlatformTarget.WASM_JS

internal actual fun getOSInformation(): OSInformation? = null