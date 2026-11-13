import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.auth.Auth
import io.supabase.auth.AuthImpl
import io.supabase.auth.BrowserBridge
import io.supabase.auth.FlowType
import io.supabase.auth.MemoryCodeVerifierCache
import io.supabase.auth.auth
import io.supabase.auth.event.AuthEvent
import io.supabase.auth.setupPlatform
import io.supabase.auth.status.SessionStatus
import io.supabase.auth.user.UserInfo
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.respondJson
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
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("") }
    ): Auth {
        supabase = createMockedSupabaseClient(
            configuration = {
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