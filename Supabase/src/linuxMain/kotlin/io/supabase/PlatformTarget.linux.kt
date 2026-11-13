package io.supabase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

internal actual fun getOSInformation(): OSInformation? = OSInformation(
    name = "Linux",
    version = getOSVersion()
)

private fun getOSVersion(): String = readProcVersion() ?: "Unknown"

@OptIn(ExperimentalForeignApi::class)
fun readProcVersion(): String? {
    val path = "/proc/version"
    val file = fopen(path, "r") ?: return null
    try {
        val buffer = ByteArray(1024)
        val bytesRead = fread(buffer.refTo(0), 1.convert(), buffer.size.convert(), file).toInt()
        return buffer.decodeToString(endIndex = bytesRead).trim()
    } finally {
        fclose(file)
    }
}
