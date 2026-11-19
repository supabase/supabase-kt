package io.supabase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.useContents

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal fun getOSVersion(): String {
    val processInfo = platform.Foundation.NSProcessInfo.processInfo
    return processInfo.operatingSystemVersion.useContents {
        val majorVersion = this.majorVersion
        val minorVersion = this.minorVersion
        val patchVersion = this.patchVersion
        buildString {
            append(majorVersion)
            append(".")
            append(minorVersion)
            append(".")
            append(patchVersion)
        }
    }
}