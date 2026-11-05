import app.cash.turbine.test
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.resetSessionStatusForBackground
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.Identity
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class AuthTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testLoadingSessionFromStorage() = runTest(timeout = 30.seconds) {
        val sessionManager = MemorySessionManager(userSession())

        client = createMockedSupabaseClient(
            configuration = {
                coroutineDispatcher = StandardTestDispatcher(testScheduler)
                install(Auth) {
                    minimalConfig()
                    this.sessionManager = sessionManager
                    autoLoadFromStorage = true
                    authScope = backgroundScope
                }
            }
        )

        client.auth.awaitInitialization()

        runCurrent()

        assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
    }

    @Test
    fun testErrorWhenUsingAccessToken() = runTest {
        assertFailsWith<IllegalStateException> {
            createMockedSupabaseClient(
                configuration = {
                    accessToken = {
                        "myToken"
                    }
                    install(Auth)
                }
            )
        }
    }

    @Test
    fun testSavingSessionToStorage() {
        runTest {
            val sessionManager = MemorySessionManager()
            client = createMockedSupabaseClient(
                configuration = {
                    install(Auth) {
                        minimalConfig()
                        this.sessionManager = sessionManager
                        autoSaveToStorage = true
                    }
                }
            )
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            assertNull(sessionManager.loadSessionOrNull())
            val session = userSession()
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            assertEquals(session, sessionManager.loadSessionOrNull())
        }
    }

    @Test
    fun testBackgroundKeepsAuthenticatedStatusWhenResetDisabled() = runTest {
        client = createMockedSupabaseClient(configuration = {
            install(Auth) {
                minimalConfig()
                resetStatusOnBackground = false
            }
        })
        client.auth.awaitInitialization()
        client.auth.importSession(userSession())
        assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
        client.auth.resetSessionStatusForBackground()
        assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
    }

    @Test
    fun testBackgroundResetsStatusToInitializingByDefault() = runTest {
        client = createMockedSupabaseClient(configuration = {
            install(Auth) {
                minimalConfig()
            }
        })
        client.auth.awaitInitialization()
        client.auth.importSession(userSession())
        assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
        client.auth.resetSessionStatusForBackground()
        assertIs<SessionStatus.Initializing>(client.auth.sessionStatus.value)
    }

    @Test
    fun testImportExpiredSession() {
        runTest {
            val newSession = userSession()
            client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalConfig()
                    alwaysAutoRefresh = true
                }
            }) {
                respondJson(newSession)
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 0)
            client.auth.importSession(session)
            client.auth.sessionStatus
                .filter { it is SessionStatus.Authenticated }
                .test {
                    val item = awaitItem() as SessionStatus.Authenticated
                    assertIs<SessionSource.Refresh>(item.source)
                    assertEquals(newSession.expiresIn, client.auth.currentSessionOrNull()?.expiresIn)
                }
            client.auth.stopAutoRefreshForCurrentSession()
        }
    }

    @Test
    fun testAutoRefreshSession() {
        runTest {
            val newSession = userSession()
            client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalConfig()
                    autoLoadFromStorage = false
                    alwaysAutoRefresh = false
                    autoSaveToStorage = false
                }
            }) {
                respondJson(newSession)
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 0)
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            assertEquals(session.expiresIn, client.auth.currentSessionOrNull()?.expiresIn) //The session shouldn't be refreshed automatically as alwaysAutoRefresh is false
            client.auth.startAutoRefreshForCurrentSession()
            client.auth.sessionStatus
                .filter { it is SessionStatus.Authenticated && it.source is SessionSource.Refresh }
                .test {
                    val item = awaitItem() as SessionStatus.Authenticated
                    assertEquals(newSession.expiresIn, item.session.expiresIn)
                }
        }
    }

    @Test
    fun testAutoRefreshFailureNetworkValidSession() {
        runTest {
            val newSession = userSession()
            client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalConfig()
                    alwaysAutoRefresh = true
                }
                defaultLogLevel = LogLevel.DEBUG
            }) {
                throw IllegalStateException("Some random error") //everything except RestException are handled as network errors
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 1)
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value) // since the session is still valid, the status should be authenticated
            client.auth.events.test(timeout = 2.seconds) { //event should be emitted regardless of the session status
                val event = awaitItem()
                assertIs<AuthEvent.RefreshFailure>(event)
                assertIs<RefreshFailureCause.NetworkError>(event.cause)
            }
            client.auth.stopAutoRefreshForCurrentSession()
        }
    }

    @Test
    fun testAutoRefreshFailureServerErrorValidSession() {
        runTest {
            client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalConfig()
                    alwaysAutoRefresh = true
                }
                defaultLogLevel = LogLevel.DEBUG
            }) {
                respondError(HttpStatusCode.InternalServerError, "{}")
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 1)
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value) // since the session is still valid, the status should be authenticated
            client.auth.events.test(timeout = 2.seconds) { //event should be emitted regardless of the session status
                val event = awaitItem()
                assertIs<AuthEvent.RefreshFailure>(event)
                assertIs<RefreshFailureCause.InternalServerError>(event.cause)
            }
            client.auth.stopAutoRefreshForCurrentSession()
        }
    }

    @Test
    fun testAutoRefreshFailureInvalidSession() {
        runTest {
            var first = true
            val newSession = userSession()
            client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalConfig()
                    alwaysAutoRefresh = false
                }
                defaultLogLevel = LogLevel.DEBUG
            }) {
                if(first) {
                    first = false
                    respondError(HttpStatusCode.InternalServerError, "{}")
                } else respondJson(newSession)
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 0)
            launch {
                client.auth.events.test(timeout = 1.seconds) { //event should be emitted regardless of the session status
                    val event = awaitItem()
                    assertIs<SessionStatus.RefreshFailure>(client.auth.sessionStatus.value) // session expired and should be in refresh failure state
                    assertIs<AuthEvent.RefreshFailure>(event)
                }
            }
            client.auth.importSession(session, autoRefresh = true)
        }
    }

    @Test
    fun testGetOAuthUrl() {
        runTest {
            val expectedProvider = Github
            val expectedRedirectUrl = "https://example.com?someParama=true&another=one" // Test that url params aren't stripped away
            val encodedRedirectUrl = "https%3A%2F%2Fexample.com%3FsomeParama%3Dtrue%26another%3Done"
            val endpoint = "authorize/custom/endpoint"
            val supabaseUrl = "https://id.supabase.co"
            client = createMockedSupabaseClient(supabaseUrl = supabaseUrl, configuration = configuration)
            client.auth.awaitInitialization()
            val url = Url(client.auth.getOAuthUrl(expectedProvider, expectedRedirectUrl, endpoint) {
                queryParams["key"] = "value.!.,with?special"
                scopes.add("scope1")
                scopes.add("scope2")
            })
            assertEquals(
                endpoint,
                url.pathAfterVersion().substring(1)
            )
            assertEquals(
                expectedProvider.name,
                url.parameters["provider"]
            )
            assertContains(
                url.toString(),
                encodedRedirectUrl
            )
            assertEquals(
                expectedRedirectUrl,
                url.parameters["redirect_to"]
            )
            assertEquals(
                "value.!.,with?special",
                url.parameters["key"]
            )
            assertContains(
                url.toString(),
                "value.%21.%2Cwith%3Fspecial"
            )
            assertEquals(
                "scope1 scope2",
                url.parameters["scopes"]
            )
        }

    }

    @Test
    fun testSessionMethods() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration)
            client.auth.awaitInitialization()
            val expectedIdentities = listOf(Identity("id", buildJsonObject {  }, provider = "provider", userId = "userId"))
            val expectedUser = UserInfo(
                id = "id",
                aud = "aud",
                identities = expectedIdentities
            )
            val session = UserSession(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expiresIn = 3600,
                tokenType = "Bearer",
                user = expectedUser
            )
            client.auth.importSession(session)
            assertEquals(session, (client.auth.sessionStatus.value as SessionStatus.Authenticated).session)
            assertEquals(session, client.auth.currentSessionOrNull())
            assertEquals(expectedUser, client.auth.currentUserOrNull())
            assertEquals(expectedIdentities, client.auth.currentIdentitiesOrNull())
            client.auth.clearSession()
            assertNull(client.auth.currentSessionOrNull())
        }
    }

    @Test
    fun testClearSession() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration)
            client.auth.awaitInitialization()
            val session = userSession()
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            client.auth.clearSession()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

}

fun userSession(customToken: String = "accessToken", expiresIn: Long = 3600, user: UserInfo? = null) = UserSession(
    accessToken = customToken,
    refreshToken = "refreshToken",
    expiresIn = expiresIn,
    tokenType = "Bearer",
    user = user
)