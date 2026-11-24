import io.supabase.auth.Auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.openExternalUrl
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.test.runTest
import platform.posix.chmod
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv
import platform.posix.mkdtemp
import platform.posix.remove
import platform.posix.setenv
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class OpenExternalUrlTest {

    @Test
    fun testUrlWithQueryParametersReachesXdgOpenIntact() = runTest {
        val url = "https://projectref.supabase.co/auth/v1/authorize?provider=google&redirect_to=http://localhost:4242&code_challenge=abc"
        val dir = memScoped { mkdtemp("/tmp/xdg-open-test-XXXXXX".cstr.getPointer(this))!!.toKString() }
        val script = "$dir/xdg-open"
        val received = "$dir/received"
        // A stand-in for xdg-open that records the argument it was given.
        writeFile(script, "#!/bin/sh\nprintf '%s' \"\$1\" > \"$received\"\n")
        chmod(script, 493u.convert()) // 0755
        val path = getenv("PATH")?.toKString()
        setenv("PATH", "$dir:$path", 1)
        val client = createMockedSupabaseClient(configuration = { install(Auth) { minimalConfig() } })
        try {
            client.openExternalUrl(url)
            assertEquals(url, readFile(received))
        } finally {
            setenv("PATH", path.orEmpty(), 1)
            client.close()
            remove(received)
            remove(script)
            remove(dir)
        }
    }

    private fun writeFile(path: String, content: String) {
        val file = fopen(path, "w") ?: error("Can't write $path")
        fputs(content, file)
        fclose(file)
    }

    private fun readFile(path: String): String = memScoped {
        val file = fopen(path, "r") ?: return@memScoped ""
        val buffer = allocArray<ByteVar>(BUFFER_SIZE)
        val text = StringBuilder()
        while (fgets(buffer, BUFFER_SIZE, file) != null) text.append(buffer.toKString())
        fclose(file)
        text.toString()
    }

    private companion object {
        const val BUFFER_SIZE = 4096
    }
}
