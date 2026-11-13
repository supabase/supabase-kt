package io.supabase

internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "Android",
    version = android.os.Build.VERSION.RELEASE
)