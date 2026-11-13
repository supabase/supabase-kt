package io.supabase

internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = System.getProperty("os.name"),
    version = System.getProperty("os.version")
)