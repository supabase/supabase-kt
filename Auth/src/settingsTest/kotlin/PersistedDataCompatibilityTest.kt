import com.russhwolf.settings.MapSettings
import io.supabase.auth.Auth
import io.supabase.auth.SettingsCodeVerifierCache
import io.supabase.auth.SettingsSessionManager
import io.supabase.auth.auth
import io.supabase.auth.createDefaultSettingsKey
import io.supabase.auth.status.SessionStatus
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Checks that data stored on user devices by an older SDK version can still be loaded,
 * so users stay signed in after updating.
 *
 * The fixtures below were produced by the published `io.github.jan-tennert.supabase:auth-kt:3.9.0` artifact
 * (default key derivation + [SettingsSessionManager] / [SettingsCodeVerifierCache]) and must never be regenerated
 * with a newer version, otherwise this test stops guarding anything.
 */
class PersistedDataCompatibilityTest {

    @Test
    fun testDefaultSettingsKeyIsUnchanged() {
        assertEquals("sb-https:--abcdefghijklmnop-supabase-co", createDefaultSettingsKey(SUPABASE_URL))
    }

    @Test
    fun testSessionPersistedBy390IsRestored() = runTest {
        val settings = MapSettings(SESSION_KEY to SESSION_390, CODE_VERIFIER_KEY to CODE_VERIFIER_390)
        val client = createMockedSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            configuration = {
                install(Auth) {
                    sessionManager = SettingsSessionManager(settings, key = SESSION_KEY)
                    codeVerifierCache = SettingsCodeVerifierCache(settings, key = CODE_VERIFIER_KEY)
                    alwaysAutoRefresh = false
                    autoLoadFromStorage = true
                }
            }
        )
        client.auth.awaitInitialization()
        val status = client.auth.sessionStatus.value
        assertIs<SessionStatus.Authenticated>(status)
        val session = status.session
        assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4ZjdhIn0.sig", session.accessToken)
        assertEquals("v1-refresh-token", session.refreshToken)
        assertEquals("8f7a6c2e-1b2c-4d5e-9f00-112233445566", assertNotNull(session.user).id)
        // An in-flight PKCE flow started on 3.9.0 must be completable on 4.x
        assertEquals(CODE_VERIFIER_390, client.auth.codeVerifierCache.loadCodeVerifier())
        client.close()
    }

    @Test
    fun testLegacyBareSessionKeyIsStillMigrated() = runTest {
        val settings = MapSettings(SettingsSessionManager.SETTINGS_KEY to SESSION_390)
        val sessionManager = SettingsSessionManager(settings, key = SESSION_KEY)
        assertEquals("v1-refresh-token", sessionManager.loadSession().refreshToken)
        assertFalse(settings.hasKey(SettingsSessionManager.SETTINGS_KEY))
    }

    private companion object {
        const val SUPABASE_URL = "https://abcdefghijklmnop.supabase.co"
        const val SESSION_KEY = "sb-https:--abcdefghijklmnop-supabase-co-session"
        const val CODE_VERIFIER_KEY = "sb-https:--abcdefghijklmnop-supabase-co-supabase_code_verifier"
        const val CODE_VERIFIER_390 = "pkce-verifier-0123456789abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJK"
        const val SESSION_390 = """{"access_token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4ZjdhIn0.sig","refresh_token":"v1-refresh-token","provider_refresh_token":"gho_refresh","provider_token":"gho_provider","expires_in":3600,"token_type":"bearer","user":{"app_metadata":{"provider":"github","providers":["email","github"],"tier":3},"aud":"authenticated","confirmation_sent_at":null,"confirmed_at":"2026-01-02T03:04:05.123456Z","created_at":"2025-12-31T23:59:59Z","email":"jane@example.com","email_confirmed_at":"2026-01-02T03:04:05.123456Z","factors":[{"id":"fac-1","created_at":"2026-02-02T00:00:00Z","updated_at":"2026-02-02T00:00:00Z","status":"verified","friendly_name":"Phone app","factor_type":"totp","last_challenged_at":null}],"id":"8f7a6c2e-1b2c-4d5e-9f00-112233445566","identities":[{"id":"12345","identity_data":{"email":"jane@example.com","sub":"12345"},"identity_id":"0f0f0f0f-0000-4000-8000-000000000001","last_sign_in_at":"2026-10-01T10:00:00Z","updated_at":"2026-10-01T10:00:00Z","created_at":"2025-12-31T23:59:59Z","provider":"github","user_id":"8f7a6c2e-1b2c-4d5e-9f00-112233445566"}],"last_sign_in_at":"2026-10-01T10:00:00Z","phone":"","role":"authenticated","updated_at":"2026-10-01T10:00:00Z","user_metadata":{"full_name":"Jane Doe","avatar_url":"https://example.com/a.png","nested":{"a":[1,2,{"b":null}]}},"phone_change_sent_at":null,"new_phone":null,"email_change_sent_at":null,"new_email":null,"invited_at":null,"recovery_sent_at":null,"phone_confirmed_at":null,"action_link":null,"is_anonymous":false,"is_sso_user":null,"banned_until":null,"deleted_at":null},"type":"","expiresAt":"2026-11-04T09:48:43.622732Z"}"""
    }

}
