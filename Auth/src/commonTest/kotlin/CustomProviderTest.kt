import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.admin.custom.provider.CustomOAuthProvider
import io.supabase.auth.admin.custom.provider.CustomProviderType
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class CustomProviderTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }

    private val sampleOIDCProvider = CustomOAuthProvider(
        id = "provider-id-1",
        providerType = CustomProviderType.OIDC,
        identifier = "custom:my-provider",
        name = "My Custom Provider",
        clientId = "client-id-123",
        issuer = "https://accounts.example.com",
        scopes = listOf("openid", "profile", "email"),
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z")
    )

    private val sampleOAuth2Provider = CustomOAuthProvider(
        id = "provider-id-2",
        providerType = CustomProviderType.OAUTH2,
        identifier = "custom:oauth2-provider",
        name = "OAuth2 Provider",
        clientId = "oauth2-client-id",
        authorizationUrl = "https://provider.example.com/authorize",
        tokenUrl = "https://provider.example.com/token",
        userinfoUrl = "https://provider.example.com/userinfo",
        scopes = listOf("profile", "email"),
        createdAt = Instant.parse("2024-01-02T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-02T00:00:00Z")
    )

    private lateinit var client: SupabaseClient

    @Test
    fun testListProvidersNoFilter() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                val params = it.url.parameters
                assertNull(params["type"])
                respondJson(
                    """{"providers": ${Json.encodeToJsonElement(listOf(sampleOIDCProvider, sampleOAuth2Provider))}}"""
                )
            }
            client.auth.awaitInitialization()
            val providers = client.auth.admin.customProviders.listProviders()
            assertEquals(2, providers.size)
            assertEquals("custom:my-provider", providers[0].identifier)
            assertEquals("custom:oauth2-provider", providers[1].identifier)
        }
    }

    @Test
    fun testListProvidersFilterByOIDC() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                val params = it.url.parameters
                assertEquals("oidc", params["type"])
                respondJson(
                    """{"providers": ${Json.encodeToJsonElement(listOf(sampleOIDCProvider))}}"""
                )
            }
            client.auth.awaitInitialization()
            val providers = client.auth.admin.customProviders.listProviders(CustomProviderType.OIDC)
            assertEquals(1, providers.size)
            assertEquals("custom:my-provider", providers[0].identifier)
            assertEquals(CustomProviderType.OIDC, providers[0].providerType)
        }
    }

    @Test
    fun testListProvidersFilterByOAuth2() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                val params = it.url.parameters
                assertEquals("oauth2", params["type"])
                respondJson(
                    """{"providers": ${Json.encodeToJsonElement(listOf(sampleOAuth2Provider))}}"""
                )
            }
            client.auth.awaitInitialization()
            val providers = client.auth.admin.customProviders.listProviders(CustomProviderType.OAUTH2)
            assertEquals(1, providers.size)
            assertEquals("custom:oauth2-provider", providers[0].identifier)
            assertEquals(CustomProviderType.OAUTH2, providers[0].providerType)
        }
    }

    @Test
    fun testCreateProviderOIDC() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("oidc", body["provider_type"]?.jsonPrimitive?.content)
                assertEquals("custom:my-provider", body["identifier"]?.jsonPrimitive?.content)
                assertEquals("My Custom Provider", body["name"]?.jsonPrimitive?.content)
                assertEquals("client-id-123", body["client_id"]?.jsonPrimitive?.content)
                assertEquals("client-secret", body["client_secret"]?.jsonPrimitive?.content)
                assertEquals("https://accounts.example.com", body["issuer"]?.jsonPrimitive?.content)
                assertEquals(setOf("openid", "profile", "email"), Json.decodeFromJsonElement(body["scopes"]!!))
                respondJson(Json.encodeToJsonElement(sampleOIDCProvider).toString())
            }
            client.auth.awaitInitialization()
            val provider = client.auth.admin.customProviders.createProvider {
                providerType = CustomProviderType.OIDC
                identifier = "custom:my-provider"
                name = "My Custom Provider"
                clientId = "client-id-123"
                clientSecret = "client-secret"
                issuer = "https://accounts.example.com"
                scopes = listOf("openid", "profile", "email")
            }
            assertEquals("custom:my-provider", provider.identifier)
            assertEquals(CustomProviderType.OIDC, provider.providerType)
        }
    }

    @Test
    fun testCreateProviderOAuth2() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("oauth2", body["provider_type"]?.jsonPrimitive?.content)
                assertEquals("custom:oauth2-provider", body["identifier"]?.jsonPrimitive?.content)
                assertEquals("https://provider.example.com/authorize", body["authorization_url"]?.jsonPrimitive?.content)
                assertEquals("https://provider.example.com/token", body["token_url"]?.jsonPrimitive?.content)
                assertEquals("https://provider.example.com/userinfo", body["userinfo_url"]?.jsonPrimitive?.content)
                assertEquals(setOf("profile", "email"), Json.decodeFromJsonElement(body["scopes"]!!))
                respondJson(Json.encodeToJsonElement(sampleOAuth2Provider).toString())
            }
            client.auth.awaitInitialization()
            val provider = client.auth.admin.customProviders.createProvider {
                providerType = CustomProviderType.OAUTH2
                identifier = "custom:oauth2-provider"
                name = "OAuth2 Provider"
                clientId = "oauth2-client-id"
                clientSecret = "oauth2-secret"
                authorizationUrl = "https://provider.example.com/authorize"
                tokenUrl = "https://provider.example.com/token"
                userinfoUrl = "https://provider.example.com/userinfo"
                scopes = listOf("profile", "email")
            }
            assertEquals("custom:oauth2-provider", provider.identifier)
            assertEquals(CustomProviderType.OAUTH2, provider.providerType)
        }
    }

    @Test
    fun testGetProvider() {
        runTest {
            val providerId = "custom:my-provider"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/$providerId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respondJson(Json.encodeToJsonElement(sampleOIDCProvider).toString())
            }
            client.auth.awaitInitialization()
            val provider = client.auth.admin.customProviders.getProvider(providerId)
            assertEquals("custom:my-provider", provider.identifier)
            assertEquals("My Custom Provider", provider.name)
            assertEquals(CustomProviderType.OIDC, provider.providerType)
        }
    }

    @Test
    fun testUpdateProvider() {
        runTest {
            val providerId = "custom:my-provider"
            val updatedName = "Updated Provider Name"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/$providerId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Put, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(updatedName, body["name"]?.jsonPrimitive?.content)
                assertEquals("https://new-issuer.example.com", body["issuer"]?.jsonPrimitive?.content)
                assertNull(body["client_secret"])
                respondJson(
                    Json.encodeToJsonElement(sampleOIDCProvider.copy(name = updatedName, issuer = "https://new-issuer.example.com")).toString()
                )
            }
            client.auth.awaitInitialization()
            val provider = client.auth.admin.customProviders.updateProvider(providerId) {
                name = updatedName
                issuer = "https://new-issuer.example.com"
            }
            assertEquals(updatedName, provider.name)
            assertEquals("https://new-issuer.example.com", provider.issuer)
        }
    }

    @Test
    fun testDeleteProvider() {
        runTest {
            val providerId = "custom:my-provider"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/$providerId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respond("")
            }
            client.auth.awaitInitialization()
            client.auth.admin.customProviders.deleteProvider(providerId)
        }
    }

    @Test
    fun testListProvidersEmptyResponse() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                respondJson("""{"providers": []}""")
            }
            client.auth.awaitInitialization()
            val providers = client.auth.admin.customProviders.listProviders()
            assertEquals(0, providers.size)
        }
    }

    @Test
    fun testCreateProviderWithOptionalFields() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/admin/custom-providers/", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals("oidc", body["provider_type"]?.jsonPrimitive?.content)

                assertNotNull(body["pkce_enabled"])
                assertNotNull(body["email_optional"])
                respondJson(Json.encodeToString(sampleOIDCProvider.copy(pkceEnabled = true, emailOptional = false)))
            }
            client.auth.awaitInitialization()
            val provider = client.auth.admin.customProviders.createProvider {
                providerType = CustomProviderType.OIDC
                identifier = "custom:my-provider"
                name = "My Provider"
                clientId = "client-id"
                clientSecret = "client-secret"
                issuer = "https://example.com"
                pkceEnabled = true
                emailOptional = false
            }
            assertEquals(true, provider.pkceEnabled)
            assertEquals(false, provider.emailOptional)
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if (::client.isInitialized) {
                client.close()
            }
        }
    }

}