import app.cash.turbine.test
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthImpl
import io.github.jan.supabase.auth.BrowserBridge
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.setupPlatform
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.logging.SupabaseLoggingProcessor
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.respondJson
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.util.PlatformUtils.IS_BROWSER
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

private const val EXAMPLE_URL = "https://example.com/"

class PlatformSetupTest {

    private lateinit var supabase: SupabaseClient

    @Test
    fun testPlatformSetupTestNoHash() = runTestOnBrowser {
        val auth = createAuthClient(autoSetup = false)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
    }

    @Test
    fun testPlatformSetupTestNoCode() = runTestOnBrowser {
        val auth = createAuthClient(autoSetup = false, flowType = FlowType.PKCE)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
    }

    @Test
    fun testPlatformSetupWithInvalidHash() = runTestOnBrowser {
        var changeUrlCalled = false
        val bridge = BrowserBridgeMock(
            hash = "abc$#as",
            changeUrl = {
                changeUrlCalled = true
            }
        )
        val auth = createAuthClient(autoSetup = false, bridge)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value);
        auth.setupPlatform()
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
        assertFalse { changeUrlCalled }
    }

    @Test
    fun testPlatformSetupWithValidSessionHash() = runTestOnBrowser {
        val expiresAt = Clock.System.now() + 100.hours //because this gets lost in the hash and doesn't matter for this test
        val session = userSession(user = UserInfo(
            id = "id",
            aud = "aud")
        ).copy(expiresAt = expiresAt)
        var changedUrl = false
        val bridge = BrowserBridgeMock(
            hash = "#access_token=${session.accessToken}&refresh_token=${session.refreshToken}&expires_in=${session.expiresIn}&token_type=${session.tokenType}&other=hash&and=another",
            changeUrl = {
                assertEquals("$EXAMPLE_URL#other=hash&and=another", it) //The other hash values are not getting removed
                changedUrl = true
            }
        )
        val auth = createAuthClient(autoSetup = false, bridge, requestHandler = {
            respondJson(session.user, Json { encodeDefaults = true })
        }) as AuthImpl
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        assertIs<SessionStatus.Authenticated>(auth.sessionStatus.value)
        assertEquals(session, (auth.sessionStatus.value as SessionStatus.Authenticated).session.copy(expiresAt = expiresAt))
        assertTrue { changedUrl }
    }

    @Test
    fun testPlatformSetupWithErrorHash() = runTestOnBrowser {
        var changedUrl = false
        val bridge = BrowserBridgeMock(
            hash = "#error_code=myCode&error_description=Description&error=Error&other=hash&and=another",
            changeUrl = {
                assertEquals("$EXAMPLE_URL#other=hash&and=another", it) //The other hash values are not getting removed
                changedUrl = true
            }
        )
        val auth = createAuthClient(autoSetup = false, bridge)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        auth.events.test {
            val errorEvent = awaitItem()
            assertIs<AuthEvent.OtpError>(errorEvent)
            assertEquals("myCode", errorEvent.error)
            assertEquals("Description (Error)", errorEvent.errorDescription)
        }
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
        assertTrue { changedUrl }
    }

    @Test
    fun testPlatformSetupWithErrorQueryImplicit() = runTestOnBrowser {
        var changedUrl = false
        val bridge = BrowserBridgeMock(
            href = "$EXAMPLE_URL?error_code=myCode&error_description=Description&error=Error&other=parameter",
            changeUrl = {
                assertEquals("$EXAMPLE_URL?other=parameter", it) //Unrelated query parameters are kept
                changedUrl = true
            }
        )
        val auth = createAuthClient(autoSetup = false, bridge, FlowType.IMPLICIT)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        auth.events.test {
            val errorEvent = awaitItem()
            assertIs<AuthEvent.OtpError>(errorEvent)
            assertEquals("myCode", errorEvent.error)
            assertEquals("Description (Error)", errorEvent.errorDescription)
        }
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
        assertTrue { changedUrl }
    }

    @Test
    fun testPlatformSetupWithValidPKCECode() = runTestOnBrowser {
        val session = userSession(user = UserInfo(
            id = "id",
            aud = "aud")
        )
        var changedUrl = false
        val bridge = BrowserBridgeMock(
            href = "$EXAMPLE_URL?code=1234&another=parameter",
            changeUrl = {
                assertEquals("$EXAMPLE_URL?another=parameter", it) //The other hash values are not getting removed
                changedUrl = true
            }
        )
        val auth = createAuthClient(
            autoSetup = false,
            bridge = bridge,
            flowType = FlowType.PKCE,
            requestHandler = {
                respondJson(session, Json { encodeDefaults = true })
            }
        )
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        assertIs<SessionStatus.Authenticated>(auth.sessionStatus.value)
        assertEquals(session, (auth.sessionStatus.value as SessionStatus.Authenticated).session)
        assertTrue { changedUrl }
    }

    @Test
    fun testPlatformSetupWithErrorCode() = runTestOnBrowser {
        var changedUrl = false
        val bridge = BrowserBridgeMock(
            href = "$EXAMPLE_URL?error_code=myCode&error_description=Description&error=Error&other=hash&and=another",
            changeUrl = {
                assertEquals("$EXAMPLE_URL?other=hash&and=another", it) //The other hash values are not getting removed
                changedUrl = true
            }
        )
        val auth = createAuthClient(autoSetup = false, bridge, FlowType.PKCE)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        auth.events.test {
            val errorEvent = awaitItem()
            assertIs<AuthEvent.OtpError>(errorEvent)
            assertEquals("myCode", errorEvent.error)
            assertEquals("Description (Error)", errorEvent.errorDescription)
        }
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
        assertTrue { changedUrl }
    }

    @Test
    fun testPlatformSetupWithSessionHashDoesNotLogTokens() = runTestOnBrowser {
        val session = userSession(customToken = "hash-access-token", user = UserInfo(
            id = "id",
            aud = "aud")
        ).copy(refreshToken = "hash-refresh-token")
        val bridge = BrowserBridgeMock(
            hash = "#access_token=${session.accessToken}&refresh_token=${session.refreshToken}&expires_in=${session.expiresIn}&token_type=${session.tokenType}"
        )
        val logs = LogCollector()
        val auth = createAuthClient(autoSetup = false, bridge, logs = logs, requestHandler = {
            respondJson(session.user, Json { encodeDefaults = true })
        })
        auth.setupPlatform()
        assertIs<SessionStatus.Authenticated>(auth.sessionStatus.value)
        assertTrue(logs.messages.any { it.startsWith("Found hash: ") })
        assertTrue(logs.messages.none { session.accessToken in it || session.refreshToken in it }, "Token logged: ${logs.messages}")
    }

    @Test
    fun testPlatformSetupWithPKCECodeDoesNotLogCode() = runTestOnBrowser {
        val session = userSession(user = UserInfo(
            id = "id",
            aud = "aud")
        )
        val code = "pkce-auth-code"
        val bridge = BrowserBridgeMock(href = "$EXAMPLE_URL?code=$code")
        val logs = LogCollector()
        val auth = createAuthClient(
            autoSetup = false,
            bridge = bridge,
            flowType = FlowType.PKCE,
            logs = logs,
            requestHandler = {
                respondJson(session, Json { encodeDefaults = true })
            }
        )
        auth.setupPlatform()
        assertIs<SessionStatus.Authenticated>(auth.sessionStatus.value)
        assertTrue(logs.messages.any { it.startsWith("Found PCKE code: ") })
        assertTrue(logs.messages.none { code in it }, "Code logged: ${logs.messages}")
    }

    @Test
    fun testPlatformSetupOnNode() = runTestOnNode {
        val auth = createAuthClient(autoSetup = false)
        assertEquals(SessionStatus.Initializing, auth.sessionStatus.value)
        auth.setupPlatform()
        assertIs<SessionStatus.NotAuthenticated>(auth.sessionStatus.value)
    }

    internal fun createAuthClient(
        autoSetup: Boolean,
        bridge: BrowserBridge = BrowserBridgeMock(),
        flowType: FlowType = FlowType.IMPLICIT,
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("") },
        logs: LogCollector? = null
    ): Auth {
        supabase = createMockedSupabaseClient(
            configuration = {
                if(logs != null) defaultLoggingFactory = { logs }
                install(Auth) {
                    autoSetupPlatform = autoSetup
                    autoLoadFromStorage = false
                    alwaysAutoRefresh = false
                    browserBridge = bridge
                    this.flowType = flowType
                    codeVerifierCache = MemoryCodeVerifierCache("verifier") //not important
                }
            },
            requestHandler = requestHandler
        )
        return supabase.auth
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::supabase.isInitialized) {
                supabase.close()
            }
        }
    }

    private fun runTestOnBrowser(body: suspend TestScope.() -> Unit) = if(IS_BROWSER) runTest(testBody = body) else runTest {}

    private fun runTestOnNode(body: suspend TestScope.() -> Unit) = if(!IS_BROWSER) runTest(testBody = body) else runTest {}

}

internal class BrowserBridgeMock(
    override val hash: String = "",
    override val href: String = "$EXAMPLE_URL$hash",
    private val changeUrl: (newUrl: String) -> Unit = {},
    private val hashChangeCallback: () -> Unit = {}
): BrowserBridge {
    override fun replaceCurrentUrl(newUrl: String) {
        this.changeUrl(newUrl)
    }

}

internal class LogCollector : SupabaseLoggingProcessor {
    val messages = mutableListOf<String>()

    override fun isEnabled(level: LogLevel): Boolean = true

    override fun processLog(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        messages.add(message)
    }
}
