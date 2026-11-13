package io.supabase.integration.auth

import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.oauth.OAuthAuthorizationDetails
import io.supabase.auth.providers.builtin.Email
import io.supabase.createSupabaseClient
import io.supabase.integration.IntegrationTestBase
import io.supabase.postgrest.Postgrest
import io.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OAuthConsentApiTest : IntegrationTestBase() {

    private fun createServiceRoleClient() = createSupabaseClient(supabaseUrl, supabaseServiceRoleKey) {
        install(Auth) {
            minimalConfig()
        }
        install(Postgrest)
        install(Storage)
    }

    private fun createHttpClient() = HttpClient(CIO) {
        followRedirects = false
    }

    @Test
    fun testApproveAuthorizationFlow() = runTest {
        val userClient = createTestClient()
        val serviceRoleClient = createServiceRoleClient()
        var createdClientId: String? = null

        try {
            val email = "oauth-consent-approve-${System.nanoTime()}@example.com"
            val password = "test-password-123!"
            userClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val redirectUri = "https://example.com/callback"
            val oauthClient = serviceRoleClient.auth.admin.oauth.createClient {
                clientName = "Integration Test Client"
                redirectUris = listOf(redirectUri)
            }
            createdClientId = oauthClient.clientId

            val accessToken = userClient.auth.currentAccessTokenOrNull()
                ?: error("No access token available for the test user")
            val authorizationId = createPendingAuthorization(
                accessToken = accessToken,
                clientId = oauthClient.clientId,
                redirectUri = redirectUri,
                scope = "email"
            )

            val detailsResponse = userClient.auth.oauth.getAuthorizationDetails(authorizationId)
            assertIs<OAuthAuthorizationDetails>(detailsResponse)
            assertEquals(oauthClient.clientId, detailsResponse.client.id)
            assertEquals("email", detailsResponse.scope)

            val approveRedirect = userClient.auth.oauth.approveAuthorization(authorizationId)
            assertTrue(approveRedirect.redirectUrl.contains("code=", ignoreCase = true))

            val grants = userClient.auth.oauth.listAuthorizationGrants()
            assertTrue(grants.any { it.client.id == oauthClient.clientId })

            userClient.auth.oauth.revokeGrant(oauthClient.clientId)

            val grantsAfterRevoke = userClient.auth.oauth.listAuthorizationGrants()
            assertTrue(grantsAfterRevoke.none { it.client.id == oauthClient.clientId })
        } finally {
            createdClientId?.let { serviceRoleClient.auth.admin.oauth.deleteClient(it) }
            userClient.close()
            serviceRoleClient.close()
        }
    }

    @Test
    fun testDenyAuthorizationFlow() = runTest {
        val userClient = createTestClient()
        val serviceRoleClient = createServiceRoleClient()
        var createdClientId: String? = null

        try {
            val email = "oauth-consent-deny-${System.nanoTime()}@example.com"
            val password = "test-password-123!"
            userClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val redirectUri = "https://example.com/callback"
            val oauthClient = serviceRoleClient.auth.admin.oauth.createClient {
                clientName = "Integration Test Client (deny)"
                redirectUris = listOf(redirectUri)
            }
            createdClientId = oauthClient.clientId

            val accessToken = userClient.auth.currentAccessTokenOrNull()
                ?: error("No access token available for the test user")
            val authorizationId = createPendingAuthorization(
                accessToken = accessToken,
                clientId = oauthClient.clientId,
                redirectUri = redirectUri,
                scope = "email"
            )

            val detailsResponse = userClient.auth.oauth.getAuthorizationDetails(authorizationId)
            assertIs<OAuthAuthorizationDetails>(detailsResponse)

            val denyRedirect = userClient.auth.oauth.denyAuthorization(authorizationId)
            assertTrue(denyRedirect.redirectUrl.contains("error=access_denied", ignoreCase = true))
        } finally {
            createdClientId?.let { serviceRoleClient.auth.admin.oauth.deleteClient(it) }
            userClient.close()
            serviceRoleClient.close()
        }
    }

    private suspend fun createPendingAuthorization(
        accessToken: String,
        clientId: String,
        redirectUri: String,
        scope: String,
    ): String {
        val httpClient = createHttpClient()
        httpClient.use { httpClient ->
            val response = httpClient.get("${supabaseUrl}/auth/v1/oauth/authorize") {
                parameter("response_type", "code")
                parameter("client_id", clientId)
                parameter("redirect_uri", redirectUri)
                parameter("scope", scope)
                parameter("state", "test-state")
                parameter("code_challenge", "a".repeat(43))
                parameter("code_challenge_method", "plain")
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $accessToken")
            }

            val location = response.headers["Location"]
                ?: error("Expected an authorization redirect to be returned")
            return parseAuthorizationId(location)
        }
    }

    private fun parseAuthorizationId(location: String): String {
        val query = URI(location).query ?: error("Authorization redirect did not contain a query")
        return query.split("&").firstNotNullOfOrNull { piece ->
            val parts = piece.split("=", limit = 2)
            if (parts.size == 2 && parts[0] == "authorization_id") {
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            } else {
                null
            }
        }
            ?: error("Authorization redirect did not contain an authorization_id query parameter")
    }
}
