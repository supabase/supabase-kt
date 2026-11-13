package io.supabase

internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "macOS",
    version = getOSVersion()
)