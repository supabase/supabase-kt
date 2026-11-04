import io.supabase.auth.setupPlatform
import io.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlatformSetupTest {

    @kotlin.test.Test
    fun testPlatformSetupTestNoAutoLoad() = runTest {
        val auth = createMinimalAuthClient(autoSetup = false) {
            it.setupPlatform()
            assertIs<SessionStatus.NotAuthenticated>(it.sessionStatus.value)
        }
    }

    @kotlin.test.Test
    fun testPlatformSetupTestAutoLoad() =  runTest {
        val auth = createMinimalAuthClient(autoSetup = true) {
            assertEquals(SessionStatus.Initializing, it.sessionStatus.value)
            it.setupPlatform()
            assertIs<SessionStatus.NotAuthenticated>(it.sessionStatus.value)
        }
    }

}