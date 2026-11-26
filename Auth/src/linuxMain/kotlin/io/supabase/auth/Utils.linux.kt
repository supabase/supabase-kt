package io.supabase.auth

import io.supabase.SupabaseClient
import io.supabase.logging.e
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.posix._exit
import platform.posix.execvp
import platform.posix.fork
import platform.posix.waitpid

/**
 * Opens [url] with `xdg-open`. The URL is passed as its own argument, not through a shell,
 * so characters like `&` in OAuth query parameters reach the browser intact.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun SupabaseClient.openExternalUrl(url: String) {
    withContext(Dispatchers.IO) {
        memScoped {
            // Built before fork: the child only calls execvp and _exit.
            val argv = allocArrayOf(XDG_OPEN.cstr.ptr, url.cstr.ptr, null)
            when (val pid = fork()) {
                -1 -> auth.logger.e { "Couldn't start $XDG_OPEN to open the url" }
                0 -> {
                    execvp(XDG_OPEN, argv)
                    _exit(EXIT_EXEC_FAILED)
                }
                else -> waitpid(pid, null, 0)
            }
        }
    }
}

private const val XDG_OPEN = "xdg-open"

// The shell's exit status for a command that could not be run.
private const val EXIT_EXEC_FAILED = 127
