import com.russhwolf.settings.MapSettings
import io.supabase.auth.SettingsCodeVerifierCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SettingsCodeVerifierCacheTest {

    @Test
    fun testMemoryCodeVerifierCache() {
        runTest {
            val codeVerifier = "codeVerifier"
            val settings = MapSettings(SettingsCodeVerifierCache.SETTINGS_KEY to codeVerifier)
            val codeVerifierCache = SettingsCodeVerifierCache(settings)
            assertEquals(codeVerifier, codeVerifierCache.loadCodeVerifier())
            assertEquals(codeVerifier, settings.getString(SettingsCodeVerifierCache.SETTINGS_KEY, ""))
            val newCodeVerifier = "newCodeVerifier"
            codeVerifierCache.saveCodeVerifier(newCodeVerifier)
            assertEquals(newCodeVerifier, codeVerifierCache.loadCodeVerifier())
            assertEquals(newCodeVerifier, settings.getString(SettingsCodeVerifierCache.SETTINGS_KEY, ""))
            codeVerifierCache.deleteCodeVerifier()
            assertNull(codeVerifierCache.loadCodeVerifier())
            assertFalse(settings.hasKey(SettingsCodeVerifierCache.SETTINGS_KEY))
        }
    }

}