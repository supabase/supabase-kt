package io.supabase.integration.auth

import io.supabase.auth.Auth
import io.supabase.auth.admin.oauth.OAuthClientTokenEndpointAuthMethod
import io.supabase.auth.admin.oauth.OAuthClientType
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.createSupabaseClient
import io.supabase.exceptions.RestException
import io.supabase.integration.IntegrationTestBase
import io.supabase.postgrest.Postgrest
import io.supabase.storage.Storage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the OAuth 2.1 client admin API.
 *
 * Requirements:
 * - Supabase CLI >= v2.84.2 (bundles GoTrue v2.188.1 with OAuth server support)
 * - [auth.oauth_server] enabled = true in config.toml
 */
class OAuthClientIntegrationTest : IntegrationTestBase() {

    private fun createServiceRoleClient() = createSupabaseClient(supabaseUrl, supabaseServiceRoleKey) {
        install(Auth) {
            minimalConfig()
        }
        install(Postgrest)
        install(Storage)
    }

    @Test
    fun testCreateAndGetClient() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "Integration Test App"
            redirectUris = listOf("https://example.com/callback")
        }

        assertNotNull(created.clientId)
        assertNotNull(created.clientSecret)
        assertEquals("Integration Test App", created.clientName)
        assertEquals(OAuthClientType.CONFIDENTIAL, created.clientType)
        assertTrue(created.redirectUris.contains("https://example.com/callback"))

        val fetched = client.auth.admin.oauth.getClient(created.clientId)
        assertEquals(created.clientId, fetched.clientId)
        assertEquals("Integration Test App", fetched.clientName)

        // Cleanup
        client.auth.admin.oauth.deleteClient(created.clientId)
    }

    @Test
    fun testListClients() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "List Test App"
            redirectUris = listOf("https://example.com/callback")
        }

        try {
            val clients = client.auth.admin.oauth.listClients()
            assertTrue(clients.isNotEmpty())
            assertTrue(clients.any { it.clientId == created.clientId })
        } finally {
            client.auth.admin.oauth.deleteClient(created.clientId)
        }
    }

    @Test
    fun testUpdateClient() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "Before Update"
            redirectUris = listOf("https://example.com/callback")
        }

        try {
            val updated = client.auth.admin.oauth.updateClient(created.clientId) {
                clientName = "After Update"
            }
            assertEquals("After Update", updated.clientName)
            assertEquals(created.clientId, updated.clientId)
        } finally {
            client.auth.admin.oauth.deleteClient(created.clientId)
        }
    }

    @Test
    fun testDeleteClient() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "To Delete"
            redirectUris = listOf("https://example.com/callback")
        }

        client.auth.admin.oauth.deleteClient(created.clientId)

        assertFailsWith<RestException> {
            client.auth.admin.oauth.getClient(created.clientId)
        }
    }

    @Test
    fun testRegenerateClientSecret() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "Regenerate Secret Test"
            redirectUris = listOf("https://example.com/callback")
        }

        try {
            val regenerated = client.auth.admin.oauth.regenerateClientSecret(created.clientId)
            assertEquals(created.clientId, regenerated.clientId)
            assertNotNull(regenerated.clientSecret)
            // Secret should be different after regeneration
            assertTrue(created.clientSecret != regenerated.clientSecret)
        } finally {
            client.auth.admin.oauth.deleteClient(created.clientId)
        }
    }

    @Test
    fun testTokenEndpointAuthMethod() = runTest {
        val client = createServiceRoleClient()
        val created = client.auth.admin.oauth.createClient {
            clientName = "Auth Method Test"
            redirectUris = listOf("https://example.com/callback")
            tokenEndpointAuthMethod = OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_POST
        }

        try {
            assertEquals(OAuthClientTokenEndpointAuthMethod.CLIENT_SECRET_POST, created.tokenEndpointAuthMethod)
        } finally {
            client.auth.admin.oauth.deleteClient(created.clientId)
        }
    }
}