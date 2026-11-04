import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandleDeeplinksTest {

    @Test
    fun testPkceDeeplinkExchangesCodeForSession() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = createClient(FlowType.PKCE) {
            requests += it
            respondJson(SESSION_JSON)
        }
        val session = CompletableDeferred<UserSession>()

        client.handleDeeplinks(NSURL(string = "test://login?code=auth-code"), onSessionSuccess = session::complete)

        assertEquals("access-token", session.await().accessToken)
        assertEquals("access-token", client.auth.currentSessionOrNull()?.accessToken)
        val request = requests.single()
        assertEquals("/token", request.url.pathAfterVersion())
        assertEquals("pkce", request.url.parameters["grant_type"])
        val body = request.body.toJsonElement().jsonObject
        assertEquals("auth-code", body["auth_code"]?.jsonPrimitive?.content)
        assertEquals("verifier", body["code_verifier"]?.jsonPrimitive?.content)
        client.close()
    }

    @Test
    fun testPkceDeeplinkReportsExchangeFailure() = runTest {
        val client = createClient(FlowType.PKCE, codeVerifier = null) {
            error("No request expected")
        }
        val failure = CompletableDeferred<Throwable>()

        client.handleDeeplinks(NSURL(string = "test://login?code=auth-code"), onError = failure::complete)

        assertTrue(failure.await() is IllegalArgumentException)
        assertNull(client.auth.currentSessionOrNull())
        client.close()
    }

    @Test
    fun testPkceDeeplinkWithErrorEmitsOtpError() = runTest {
        val client = createClient(FlowType.PKCE) {
            error("No request expected")
        }

        client.handleDeeplinks(NSURL(string = "test://login?error=access_denied&error_code=otp_expired&error_description=Link%20expired"))

        val event = client.auth.events.first()
        assertEquals(AuthEvent.OtpError("otp_expired", "Link expired (access_denied)"), event)
        client.close()
    }

    @Test
    fun testImplicitDeeplinkImportsSessionFromFragment() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = createClient(FlowType.IMPLICIT) {
            requests += it
            respondJson(USER_JSON)
        }
        val session = CompletableDeferred<UserSession>()

        client.handleDeeplinks(
            NSURL(string = "test://login#access_token=access-token&refresh_token=refresh-token&expires_in=3600&token_type=bearer"),
            onSessionSuccess = session::complete
        )

        assertEquals("refresh-token", session.await().refreshToken)
        assertEquals("access-token", client.auth.currentSessionOrNull()?.accessToken)
        assertEquals("/user", requests.single().url.pathAfterVersion())
        client.close()
    }

    @Test
    fun testDeeplinkWithOtherSchemeOrHostIsIgnored() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = createClient(FlowType.PKCE) {
            requests += it
            respondJson(SESSION_JSON)
        }

        client.handleDeeplinks(NSURL(string = "other://login?code=auth-code"))
        client.handleDeeplinks(NSURL(string = "test://other?code=auth-code"))

        assertTrue(requests.isEmpty())
        assertNull(client.auth.currentSessionOrNull())
        client.close()
    }

    private suspend fun createClient(
        flowType: FlowType,
        codeVerifier: String? = "verifier",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): SupabaseClient {
        val client = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                    scheme = "test"
                    host = "login"
                    this.flowType = flowType
                    codeVerifierCache = MemoryCodeVerifierCache(codeVerifier)
                }
            },
            requestHandler = handler
        )
        client.auth.awaitInitialization()
        return client
    }

    private companion object {
        const val USER_JSON = """{"id":"id","aud":"aud"}"""
        const val SESSION_JSON = """{"access_token":"access-token","refresh_token":"refresh-token","token_type":"bearer","expires_in":3600,"user":$USER_JSON}"""
    }

}
