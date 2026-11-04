import io.supabase.auth.setupPlatform
import io.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.assertIs

class PlatformSetupTest {

    @kotlin.test.Test
    fun testPlatformSetupTestNoAutoLoad() = runTest {
        val auth = createMinimalAuthClient(autoSetup = false) //Note: this does not test the lifecycle callbacks
        auth.setupPlatform()
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
    }

}