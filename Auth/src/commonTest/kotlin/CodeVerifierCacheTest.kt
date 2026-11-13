import io.supabase.auth.MemoryCodeVerifierCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodeVerifierCacheTest {

    @Test
    fun testMemoryCodeVerifierCache() {
        runTest {
            val codeVerifier = "codeVerifier"
            val codeVerifierCache = MemoryCodeVerifierCache(codeVerifier)
            assertEquals(codeVerifier, codeVerifierCache.loadCodeVerifier())
            val newCodeVerifier = "newCodeVerifier"
            codeVerifierCache.saveCodeVerifier(newCodeVerifier)
            assertEquals(newCodeVerifier, codeVerifierCache.loadCodeVerifier())
            codeVerifierCache.deleteCodeVerifier()
            assertNull(codeVerifierCache.loadCodeVerifier())
        }
    }

}