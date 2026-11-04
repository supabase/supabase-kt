package io.supabase


internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "tvOS",
    version = getOSVersion()
)