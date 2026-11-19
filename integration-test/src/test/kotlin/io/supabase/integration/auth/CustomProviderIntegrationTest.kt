package io.supabase.integration.auth

import io.supabase.SupabaseClient
import io.supabase.auth.Auth
import io.supabase.auth.admin.custom.provider.CustomOAuthProvider
import io.supabase.auth.admin.custom.provider.CustomProviderType
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.createSupabaseClient
import io.supabase.integration.IntegrationTestBase
import io.supabase.postgrest.Postgrest
import io.supabase.storage.Storage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CustomProviderIntegrationTest: IntegrationTestBase() {

    private fun createServiceRoleClient() = createSupabaseClient(supabaseUrl, supabaseServiceRoleKey) {
        install(Auth) {
            minimalConfig()
        }
        install(Postgrest)
        install(Storage)
    }

    @Test
    fun testCreateAndTestClientOAuth2() = runTest {
        val client = createServiceRoleClient()
        val provider = client.auth.admin.customProviders.createProvider {
            providerType = CustomProviderType.OAUTH2
            identifier = "custom:local-dev-mock"
            name = "Local Development Provider"
            clientId = "dev-client-id-12345"
            clientSecret = "dev-secret-super-safe-67890"
            authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth"
            tokenUrl = "https://oauth2.googleapis.com/token"
            userinfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo"
            scopes = listOf("profile", "email")
        }
        try {
            assertEquals("custom:local-dev-mock", provider.identifier)
            assertEquals(CustomProviderType.OAUTH2, provider.providerType)
            assertEquals("Local Development Provider", provider.name)
            assertEquals("dev-client-id-12345", provider.clientId)
            assertEquals("https://accounts.google.com/o/oauth2/v2/auth", provider.authorizationUrl)
            assertEquals("https://oauth2.googleapis.com/token", provider.tokenUrl)
            assertEquals("https://www.googleapis.com/oauth2/v3/userinfo", provider.userinfoUrl)
            assertEquals(setOf("profile", "email"), provider.scopes?.toSet())
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
    }

    @Test
    fun testCreateAndTestClientOIDC() = runTest {
        val client = createServiceRoleClient()
        val provider = client.auth.admin.customProviders.createProvider {
            providerType = CustomProviderType.OIDC
            identifier = "custom:local-dev-mock"
            name = "Local Development Provider"
            clientId = "dev-client-id-12345"
            clientSecret = "dev-secret-super-safe-67890"
            issuer = "https://accounts.google.com"
            scopes = listOf("profile", "email")
        }
        try {
            assertEquals("custom:local-dev-mock", provider.identifier)
            assertEquals(CustomProviderType.OIDC, provider.providerType)
            assertEquals("Local Development Provider", provider.name)
            assertEquals("dev-client-id-12345", provider.clientId)
            assertEquals("https://accounts.google.com", provider.issuer)
            assertEquals(setOf("profile", "email", "openid"), provider.scopes?.toSet())
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
    }

    @Test
    fun testCreateAndGetClient() = runTest {
        val client = createServiceRoleClient()
        val provider = client.createProvider()
        val fetchedProvider = client.auth.admin.customProviders.getProvider(provider.identifier)
        try {
            assertEquals(provider, fetchedProvider)
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
    }

    @Test
    fun testListProviderNoType() = runTest {
        val client = createServiceRoleClient()
        val provider = client.createProvider()
        val fetchedProvider = client.auth.admin.customProviders.listProviders()
        try {
        assertEquals(listOf(provider), fetchedProvider)
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
        assertEquals(0, client.auth.admin.customProviders.listProviders().size)
    }

    @Test
    fun testListProviderOIDC() = runTest {
        val client = createServiceRoleClient()
        val provider = client.createProvider()
        val fetchedProvider = client.auth.admin.customProviders.listProviders(CustomProviderType.OIDC)
        try {
        assertEquals(listOf(provider), fetchedProvider)
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
        assertEquals(0, client.auth.admin.customProviders.listProviders().size)
    }

    @Test
    fun testListProviderOAuth2() = runTest {
        val client = createServiceRoleClient()
        val provider = client.createProvider()
        val fetchedProvider = client.auth.admin.customProviders.listProviders(CustomProviderType.OAUTH2)
        try {
        assertEquals(0, fetchedProvider.size)
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
        assertEquals(0, client.auth.admin.customProviders.listProviders().size)
    }

    @Test
    fun testUpdateProvider() = runTest {
        val client = createServiceRoleClient()
        val provider = client.createProvider()
        try {
            val updated = client.auth.admin.customProviders.updateProvider(provider.identifier) {
                name = "Test"
            }
            assertEquals("Test", updated.name)
        } finally {
            client.auth.admin.customProviders.deleteProvider(provider.identifier)
        }
        assertEquals(0, client.auth.admin.customProviders.listProviders().size)
    }


    private suspend fun SupabaseClient.createProvider(): CustomOAuthProvider {
        return auth.admin.customProviders.createProvider {
            providerType = CustomProviderType.OIDC
            identifier = "custom:local-dev-mock"
            name = "Local Development Provider"
            clientId = "dev-client-id-12345"
            clientSecret = "dev-secret-super-safe-67890"
            issuer = "https://accounts.google.com"
            scopes = listOf("profile", "email")
        }
    }

}