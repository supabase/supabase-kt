import io.supabase.auth.Auth
import io.supabase.auth.FileCodeVerifierCache
import io.supabase.auth.FileSessionManager
import io.supabase.auth.MemorySessionManager
import io.supabase.auth.auth
import io.supabase.auth.createDefaultCodeVerifierCache
import io.supabase.auth.createDefaultSessionManager
import io.supabase.auth.createDefaultSettingsKey
import io.supabase.auth.exception.NoSessionFoundException
import io.supabase.auth.minimalConfig
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv
import platform.posix.mkdtemp
import platform.posix.setenv
import platform.posix.stat
import platform.posix.unsetenv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class FileStorageTest {

    private lateinit var directory: Path
    private val savedEnvironment = mutableMapOf<String, String?>()

    @BeforeTest
    fun setUp() {
        directory = memScoped { mkdtemp("/tmp/supabase-file-storage-XXXXXX".cstr.getPointer(this))!!.toKString() }.toPath()
        listOf(XDG_DATA_HOME, HOME).forEach { savedEnvironment[it] = getenv(it)?.toKString() }
    }

    @AfterTest
    fun tearDown() {
        savedEnvironment.forEach { (name, value) -> if (value == null) unsetenv(name) else setenv(name, value, 1) }
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    @Test
    fun testSessionIsSavedLoadedAndDeleted() = runTest {
        val file = directory / "nested" / "session.json"
        val session = userSession()
        FileSessionManager(file).saveSession(session)
        assertEquals(session, FileSessionManager(file).loadSession())
        assertEquals(FILE_MODE, modeOf(file))
        assertEquals(DIRECTORY_MODE, modeOf(file.parent!!))
        FileSessionManager(file).deleteSession()
        assertFalse(FileSystem.SYSTEM.exists(file))
        assertFailsWith<NoSessionFoundException> { FileSessionManager(file).loadSession() }
    }

    @Test
    fun testAnExistingDirectoryKeepsItsMode() = runTest {
        val existing = directory / "existing"
        FileSystem.SYSTEM.createDirectory(existing)
        val mode = modeOf(existing)
        FileSessionManager(existing / "session.json").saveSession(userSession())
        assertEquals(mode, modeOf(existing))
    }

    @Test
    fun testSavingAgainReplacesTheSession() = runTest {
        val file = directory / "session.json"
        val manager = FileSessionManager(file)
        manager.saveSession(userSession(customToken = "first"))
        manager.saveSession(userSession(customToken = "second"))
        assertEquals("second", manager.loadSession().accessToken)
        assertEquals(listOf(file), FileSystem.SYSTEM.list(directory))
    }

    @Test
    fun testCodeVerifierIsSavedLoadedAndDeleted() = runTest {
        val file = directory / "code-verifier"
        FileCodeVerifierCache(file).saveCodeVerifier("verifier")
        assertEquals("verifier", FileCodeVerifierCache(file).loadCodeVerifier())
        assertEquals(FILE_MODE, modeOf(file))
        FileCodeVerifierCache(file).deleteCodeVerifier()
        assertNull(FileCodeVerifierCache(file).loadCodeVerifier())
    }

    @Test
    fun testDefaultsKeepTheSessionUnderXdgDataHome() = runTest {
        setenv(XDG_DATA_HOME, directory.toString(), 1)
        val client = createMockedSupabaseClient(configuration = { install(Auth) { minimalConfig() } })
        val key = createDefaultSettingsKey(client.supabaseUrl)
        val session = userSession()
        client.auth.createDefaultSessionManager().saveSession(session)
        client.auth.createDefaultCodeVerifierCache().saveCodeVerifier("verifier")
        assertTrue(FileSystem.SYSTEM.exists(directory / "supabase-kt" / "$key-${FileSessionManager.FILE_SUFFIX}"))
        assertTrue(FileSystem.SYSTEM.exists(directory / "supabase-kt" / "$key-${FileCodeVerifierCache.FILE_SUFFIX}"))
        // A new client for the same project finds them.
        val nextClient = createMockedSupabaseClient(configuration = { install(Auth) { minimalConfig() } })
        assertEquals(session, nextClient.auth.createDefaultSessionManager().loadSession())
        assertEquals("verifier", nextClient.auth.createDefaultCodeVerifierCache().loadCodeVerifier())
        client.close()
        nextClient.close()
    }

    @Test
    fun testDefaultsFallBackToHomeWhenXdgDataHomeIsRelative() = runTest {
        setenv(XDG_DATA_HOME, "relative/path", 1)
        setenv(HOME, directory.toString(), 1)
        val client = createMockedSupabaseClient(configuration = { install(Auth) { minimalConfig() } })
        client.auth.createDefaultSessionManager().saveSession(userSession())
        assertTrue(FileSystem.SYSTEM.exists(directory / ".local" / "share" / "supabase-kt"))
        client.close()
    }

    @Test
    fun testDefaultsKeepTheSessionInMemoryWithoutHome() = runTest {
        unsetenv(XDG_DATA_HOME)
        unsetenv(HOME)
        val client = createMockedSupabaseClient(configuration = { install(Auth) { minimalConfig() } })
        assertIs<MemorySessionManager>(client.auth.createDefaultSessionManager())
        client.close()
    }

    private fun modeOf(path: Path): Int = memScoped {
        val status = alloc<stat>()
        stat(path.toString(), status.ptr)
        status.st_mode.toInt() and PERMISSION_BITS
    }

    private companion object {
        const val XDG_DATA_HOME = "XDG_DATA_HOME"
        const val HOME = "HOME"
        const val FILE_MODE = 384 // 0600
        const val DIRECTORY_MODE = 448 // 0700
        const val PERMISSION_BITS = 511 // 0777
    }
}
