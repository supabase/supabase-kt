import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.admin.oauth.CreateOAuthClientBuilder
import io.supabase.auth.admin.oauth.OAuthClient
import io.supabase.auth.admin.oauth.OAuthClientGrantType
import io.supabase.auth.admin.oauth.OAuthClientResponseType
import io.supabase.auth.admin.oauth.OAuthClientTokenEndpointAuthMethod
import io.supabase.auth.admin.oauth.OAuthClientType
import io.supabase.auth.admin.oauth.UpdateOAuthClientBuilder
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.testing.assertMethodIs
import io.supabase.testing.assertPathIs
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.pathAfterVersion
import io.supabase.testing.respondJson
import io.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OAuthClientApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }

    private val sampleClient = OAuthClient(
        clientId = "test-client-id",
        clientSecret = "test-secret",
        clientName = "Test App",
        clientType = OAuthClientType.CONFIDENTIAL,
        redirectUris = listOf("https://example.com/callback"),
        grantTypes = listOf(OAuthClientGrantType.AUTHORIZATION_CODE, OAuthClientGrantType.REFRESH_TOKEN),
        responseTypes = listOf(OAuthClientResponseType.CODE),
        tokenEndpointAuthMethod = OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_BASIC,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    @Test
    fun testListClients() {
        runTest {
            val page = 2
            val perPage = 10
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                val params = it.url.parameters
                assertEquals(page.toString(), params["page"])
                assertEquals(perPage.toString(), params["per_page"])
                respondJson(
                    """{"clients": ${Json.encodeToString(listOf(sampleClient))}}"""
                )
            }
            val clients = client.auth.admin.oauth.listClients(page = page, perPage = perPage)
            assertEquals(1, clients.size)
            assertEquals("Test App", clients.first().clientName)
            assertEquals(OAuthClientType.CONFIDENTIAL, clients.first().clientType)
        }
    }

    @Test
    fun testCreateClient() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("My App", body["client_name"]?.jsonPrimitive?.content)
                assertEquals("client_secret_post", body["token_endpoint_auth_method"]?.jsonPrimitive?.content)
                respondJson(
                    Json.encodeToString(sampleClient)
                )
            }
            val oauthClient = client.auth.admin.oauth.createClient {
                clientName = "My App"
                redirectUris = listOf("https://example.com/callback")
                tokenEndpointAuthMethod = OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_POST
            }
            assertEquals("test-client-id", oauthClient.clientId)
        }
    }

    @Test
    fun testGetClient() {
        runTest {
            val clientId = "test-client-id"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients/$clientId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respondJson(
                    Json.encodeToString(sampleClient)
                )
            }
            val oauthClient = client.auth.admin.oauth.getClient(clientId)
            assertEquals("Test App", oauthClient.clientName)
            assertEquals(OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_BASIC, oauthClient.tokenEndpointAuthMethod)
        }
    }

    @Test
    fun testUpdateClient() {
        runTest {
            val clientId = "test-client-id"
            val updatedName = "Updated App"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients/$clientId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Put, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(updatedName, body["client_name"]?.jsonPrimitive?.content)
                assertNull(body["logo_uri"]) // null fields should be omitted
                respondJson(
                    Json.encodeToString(sampleClient.copy(clientName = updatedName))
                )
            }
            val oauthClient = client.auth.admin.oauth.updateClient(clientId) {
                clientName = updatedName
            }
            assertEquals(updatedName, oauthClient.clientName)
        }
    }

    @Test
    fun testDeleteClient() {
        runTest {
            val clientId = "test-client-id"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients/$clientId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respond("")
            }
            client.auth.admin.oauth.deleteClient(clientId)
        }
    }

    @Test
    fun testRegenerateClientSecret() {
        runTest {
            val clientId = "test-client-id"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/oauth/clients/$clientId/regenerate_secret", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                respondJson(
                    Json.encodeToString(sampleClient.copy(clientSecret = "new-secret"))
                )
            }
            val oauthClient = client.auth.admin.oauth.regenerateClientSecret(clientId)
            assertEquals("new-secret", oauthClient.clientSecret)
        }
    }

    @Test
    fun testOAuthClientDeserialization() {
        val json = """
            {
                "client_id": "cid",
                "client_secret": "secret",
                "client_name": "App",
                "client_type": "public",
                "redirect_uris": ["https://example.com"],
                "grant_types": ["authorization_code"],
                "response_types": ["code"],
                "token_endpoint_auth_method": "none",
                "registration_type": "dynamic",
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()
        val client = Json.decodeFromString<OAuthClient>(json)
        assertEquals("cid", client.clientId)
        assertEquals(OAuthClientType.PUBLIC, client.clientType)
        assertEquals(OAuthClientGrantType.AUTHORIZATION_CODE, client.grantTypes.first())
        assertEquals(OAuthClientResponseType.CODE, client.responseTypes.first())
        assertEquals(OAuthClientTokenEndpointAuthMethod.NONE, client.tokenEndpointAuthMethod)
    }

    @Test
    fun testCreateOAuthClientBuilderSerialization() {
        val builder = CreateOAuthClientBuilder(
            clientName = "Test",
            redirectUris = listOf("https://example.com"),
            tokenEndpointAuthMethod = OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_POST
        )
        val json = Json.encodeToJsonElement(builder).jsonObject
        assertEquals("Test", json["client_name"]?.jsonPrimitive?.content)
        assertEquals("client_secret_post", json["token_endpoint_auth_method"]?.jsonPrimitive?.content)
        assertNull(json["client_uri"]) // null fields omitted with encodeDefaults=false
    }

    @Test
    fun testUpdateOAuthClientBuilderSerialization() {
        val builder = UpdateOAuthClientBuilder(
            clientName = "Updated",
            tokenEndpointAuthMethod = OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_BASIC
        )
        val json = Json { encodeDefaults = false }.encodeToJsonElement(builder).jsonObject
        assertEquals("Updated", json["client_name"]?.jsonPrimitive?.content)
        assertEquals("client_secret_basic", json["token_endpoint_auth_method"]?.jsonPrimitive?.content)
        assertNull(json["logo_uri"])
        assertNull(json["redirect_uris"])
    }
}
