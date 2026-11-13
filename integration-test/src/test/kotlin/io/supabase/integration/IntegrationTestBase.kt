package io.supabase.integration

import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.providers.builtin.Email
import io.supabase.createSupabaseClient
import io.supabase.functions.Functions
import io.supabase.postgrest.Postgrest
import io.supabase.storage.Storage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach

abstract class IntegrationTestBase {

    companion object {
        val supabaseUrl: String = System.getenv("SUPABASE_URL") ?: "http://127.0.0.1:54321"
        val supabaseAnonKey: String = System.getenv("SUPABASE_ANON_KEY")
            ?: error("SUPABASE_ANON_KEY environment variable is required")
        val supabaseServiceRoleKey: String = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
            ?: error("SUPABASE_SERVICE_ROLE_KEY environment variable is required")
    }

    private val clients = mutableListOf<SupabaseClient>()

    fun createTestClient(
        key: String = supabaseAnonKey,
        configure: SupabaseClientBuilder.() -> Unit = {}
    ): SupabaseClient {
        val client = createSupabaseClient(supabaseUrl, key) {
            install(Auth) {
                alwaysAutoRefresh = false
            }
            install(Functions)
            install(Postgrest)
            install(Storage)
            configure()
        }
        clients.add(client)
        return client
    }

    fun createStatelessClient(
        key: String = supabaseServiceRoleKey,
        configure: SupabaseClientBuilder.() -> Unit = {}
    ): SupabaseClient {
        val client = createSupabaseClient(supabaseUrl, key) {
            install(Auth) {
                minimalConfig()
            }
            install(Storage)
            configure()
        }
        clients.add(client)
        return client
    }

    suspend fun createAuthenticatedClient(configure: SupabaseClientBuilder.() -> Unit = {}): SupabaseClient {
        val client = createTestClient(configure = configure)
        val email = "test-${System.nanoTime()}@example.com"
        val password = "test-password-123!"
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return client
    }

    @AfterEach
    fun closeClients() = runBlocking {
        clients.forEach { it.close() }
        clients.clear()
    }
}
