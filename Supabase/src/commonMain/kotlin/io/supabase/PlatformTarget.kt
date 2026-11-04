@file:Suppress("UndocumentedPublicProperty")
package io.supabase

/**
 * Represents a target platform
 */
enum class PlatformTarget {
    JVM, ANDROID, JS, WASM_JS, IOS, WINDOWS, MACOS, TVOS, WATCHOS, LINUX;
}

data class OSInformation(
    val name: String,
    val version: String?
) {

    companion object {
        val CURRENT by lazy {
            try {
                getOSInformation()
            } catch (e: Exception) {
                null
            }
        }
    }

}

/**
 * The current target platform
 */
expect val CurrentPlatformTarget: PlatformTarget

/**
 * The current operating system information
 */
internal expect fun getOSInformation(): OSInformation?
