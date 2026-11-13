package io.supabase

import platform.windows.DWORD
import platform.windows.GetVersion

internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "Windows",
    version = getOSVersion()
)

private fun getOSVersion(): String {
    val dwVersion: DWORD = GetVersion()

    val dwMajorVersion = dwVersion and 255u
    val dwMinorVersion = (dwVersion shr 8) and 255u
    return "$dwMajorVersion.$dwMinorVersion"
}