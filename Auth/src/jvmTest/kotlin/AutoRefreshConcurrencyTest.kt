import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AutoRefreshConcurrencyTest {

    @Test
    fun testConcurrentSessionImportsLeaveOneAutoRefreshJob() = runTest(timeout = 60.seconds) {
        val authScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val client = createMockedSupabaseClient(configuration = {
            install(Auth) {
                minimalConfig()
                this.authScope = authScope
            }
        })
        client.auth.awaitInitialization()
        val activeJobs = { authScope.coroutineContext.job.children.count { it.isActive } }
        val jobsBefore = activeJobs()

        // Several refreshes finishing at once all import a session with auto refresh on, from
        // different threads. Each import replaces the auto refresh job, so one must be left.
        repeat(ROUNDS) { round ->
            val start = CompletableDeferred<Unit>()
            val imports = List(CALLERS) {
                launch(Dispatchers.Default) {
                    start.await()
                    client.auth.importSession(userSession(), autoRefresh = true)
                }
            }
            start.complete(Unit)
            imports.joinAll()
            assertEquals(jobsBefore + 1, activeJobs(), "Active auto refresh jobs after round $round")
        }

        client.auth.stopAutoRefreshForCurrentSession()
        assertEquals(jobsBefore, activeJobs())
        authScope.cancel()
        client.close()
    }

    private companion object {
        const val ROUNDS = 200
        const val CALLERS = 8
    }

}
