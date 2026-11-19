import io.supabase.auth.MemorySessionManager
import io.supabase.auth.exception.NoSessionFoundException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class MemorySessionManagerTest {

    @Test
    fun testMemorySessionManager() {
        runTest {
            val session = userSession()
            val sessionManager = MemorySessionManager(session)
            assertEquals(session, sessionManager.loadSessionOrNull()) //Check if the session is loaded correctly
            val newSession = userSession(expiresIn = 200)
            sessionManager.saveSession(newSession)
            assertEquals(newSession, sessionManager.loadSessionOrNull()) //Check if the new session is saved correctly
            assertNotEquals(session, sessionManager.loadSessionOrNull()) //Check if the new session is different from the old session
            sessionManager.deleteSession()
            assertNull(sessionManager.loadSessionOrNull()) //Check if the session is deleted correctly
        }
    }

    @Test
    fun testLoadSessionThrowsNoSessionFoundExceptionWhenEmpty() {
        runTest {
            val sessionManager = MemorySessionManager()
            assertFailsWith<NoSessionFoundException> { sessionManager.loadSession() }
        }
    }

}