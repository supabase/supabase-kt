package io.supabase


internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "iOS",
    version = getOSVersion()
)