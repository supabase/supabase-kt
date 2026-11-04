import io.supabase.auth.setupPlatform
import io.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.assertIs

class PlatformSetupTest {

    @kotlin.test.Test
    fun testPlatformSetupTestNoAutoLoad() = runTest {
        createMinimalAuthClient(autoSetup = false) {
            it.setupPlatform()
            assertIs<SessionStatus.NotAuthenticated>(it.sessionStatus.value)
        }
    }

}