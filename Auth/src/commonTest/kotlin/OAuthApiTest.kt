import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.auth.oauth.OAuthApiImpl
import io.supabase.auth.oauth.OAuthAuthorizationClient
import io.supabase.auth.oauth.OAuthAuthorizationDetails
import io.supabase.auth.oauth.OAuthAuthorizationUser
import io.supabase.auth.oauth.OAuthRedirect
import io.supabase.testing.MockedHttpClient
import io.supabase.testing.respondJson
import io.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

class OAuthApiTest {

    @Test
    fun testGetAuthDetail() = runTest {
        val authId = "12345"
        val data = buildJsonObject {
            put("authorization_id", authId)
            put("redirect_uri", "someUrl.com")
            put("scope", "openid profile email")
            putJsonObject("user") {
                put("id", "someUuid")
                put("email", "some@email.com")
            }
            putJsonObject("client") {
                put("id", "someClientId")
                put("name", "someClientName")
                put("uri", "someClientUri")
                put("logo_uri", "someLogoUri")
            }
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/oauth/authorizations/$authId", it.url.toString())
                assertEquals(HttpMethod.Get, it.method)
                respondJson(data.toString())
            }
        )
        val oauthApi = OAuthApiImpl(api)
        val response = oauthApi.getAuthorizationDetails(authId)
        assertIs<OAuthAuthorizationDetails>(response)
        assertEquals(authId, response.authorizationId)
        assertEquals("someUrl.com", response.redirectUri)
        assertEquals("openid profile email", response.scope)
        assertEquals(OAuthAuthorizationUser("someUuid", "some@email.com"), response.user)
        assertEquals(OAuthAuthorizationClient(
            "someClientId",
            "someClientName",
            "someClientUri",
            "someLogoUri"
        ), response.client)
    }

    @Test
    fun testGetAuthDetailRedirect() = runTest {
        val authId = "12345"
        val data = buildJsonObject {
            put("redirect_url", "someUrl.com")
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/oauth/authorizations/$authId", it.url.toString())
                assertEquals(HttpMethod.Get, it.method)
                respondJson(data.toString())
            }
        )
        val oauthApi = OAuthApiImpl(api)
        val response = oauthApi.getAuthorizationDetails(authId)
        assertIs<OAuthRedirect>(response)
        assertEquals("someUrl.com", response.redirectUrl)
    }

    @Test
    fun testApprove() = runTest {
        val authId = "12345"
        val data = buildJsonObject {
            put("redirect_url", "someUrl.com")
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/oauth/authorizations/$authId/consent", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("approve", body["action"]!!.jsonPrimitive.content)
                respondJson(data.toString())
            }
        )
        val oauthApi = OAuthApiImpl(api)
        val response = oauthApi.approveAuthorization(authId)
        assertEquals("someUrl.com", response.redirectUrl)
    }

    @Test
    fun testDeny() = runTest {
        val authId = "12345"
        val data = buildJsonObject {
            put("redirect_url", "someUrl.com")
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/oauth/authorizations/$authId/consent", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("deny", body["action"]!!.jsonPrimitive.content)
                respondJson(data.toString())
            }
        )
        val oauthApi = OAuthApiImpl(api)
        val response = oauthApi.denyAuthorization(authId)
        assertEquals("someUrl.com", response.redirectUrl)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testListGrants() = runTest {
        val granted = Clock.System.now()
        val data = buildJsonArray {
            addJsonObject {
                putJsonObject("client") {
                    put("id", "someClientId")
                    put("name", "someClientName")
                    put("uri", "someClientUri")
                    put("logo_uri", "someLogoUri")
                }
                putJsonArray("scopes") {
                    addAll(listOf("scope1", "scope2"))
                }
                put("granted_at", granted.toString())
            }
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/user/oauth/grants", it.url.toString())
                assertEquals(HttpMethod.Get, it.method)
                respondJson(data.toString())
            }
        )
        val oauthApi = OAuthApiImpl(api)
        val response = oauthApi.listAuthorizationGrants()
        assertEquals(1, response.size)
        val grant = response.first()
        assertEquals(OAuthAuthorizationClient(
            "someClientId",
            "someClientName",
            "someClientUri",
            "someLogoUri"
        ), grant.client)
        assertContentEquals(listOf("scope1", "scope2"), grant.scopes)
        assertEquals(granted, grant.grantedAt)
    }

    @Test
    fun testRevoke() = runTest {
        val clientId = "12345"
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/user/oauth/grants?client_id=$clientId", it.url.toString())
                assertEquals(HttpMethod.Delete, it.method)
                respond("")
            }
        )
        val oauthApi = OAuthApiImpl(api)
        oauthApi.revokeGrant(clientId)
    }

}