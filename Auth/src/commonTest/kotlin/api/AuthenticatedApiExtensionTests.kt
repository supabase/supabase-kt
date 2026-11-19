package api

import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.api.authenticatedSupabaseApi
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.user.UserSession
import io.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticatedApiExtensionTests {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth.Companion) {
            minimalConfig()
        }
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testBaseUrlOverloadWithBuilderOptions() = runTest {
        val expectedToken = "test-access-token"
        val expectedHeaderName = "X-Builder-Header"
        val expectedHeaderValue = "builder-value"

        client = createMockedSupabaseClient(configuration = configuration) {
            assertEquals("Bearer $expectedToken", it.headers["Authorization"])
            assertEquals(expectedHeaderValue, it.headers[expectedHeaderName])
            assertTrue(it.url.toString().contains("https://api.example.com/test"))
            respond("")
        }

        client.auth.awaitInitialization()
        client.auth.importSession(createSession(expectedToken))

        val api = client.authenticatedSupabaseApi(
            baseUrl = "https://api.example.com/",
            requireSession = true
        ) {
            defaultRequest = {
                header(expectedHeaderName, expectedHeaderValue)
            }
        }

        api.get("test")
    }

    @Test
    fun testResolveExtensionAddsPathPrefix() = runTest {
        val expectedToken = "test-access-token"

        client = createMockedSupabaseClient(configuration = configuration) {
            assertEquals("Bearer $expectedToken", it.headers["Authorization"])
            assertTrue(it.url.toString().contains("https://api.example.com/nested/test"))
            respond("")
        }

        client.auth.awaitInitialization()
        client.auth.importSession(createSession(expectedToken))

        val api = client.authenticatedSupabaseApi(
            baseUrl = "https://api.example.com/",
            requireSession = true
        ) {}

        api.resolve("nested").get("test")
    }

    @Test
    fun testWithDefaultRequestMergesHeaders() = runTest {
        val expectedToken = "test-access-token"
        val firstHeader = "X-Default-1"
        val secondHeader = "X-Default-2"

        client = createMockedSupabaseClient(configuration = configuration) {
            assertEquals("Bearer $expectedToken", it.headers["Authorization"])
            assertEquals("a", it.headers[firstHeader])
            assertEquals("b", it.headers[secondHeader])
            respond("")
        }

        client.auth.awaitInitialization()
        client.auth.importSession(createSession(expectedToken))

        val api = client.authenticatedSupabaseApi(
            baseUrl = "https://api.example.com/",
            requireSession = true
        ) {
            defaultRequest = { header(firstHeader, "a") }
        }.withDefaultRequest {
            header(secondHeader, "b")
        }

        api.get("test")
    }

    private fun createSession(
        accessToken: String,
        expiresIn: Long = 3600,
        refreshToken: String = "refresh-token"
    ) = UserSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        tokenType = "Bearer",
        user = null
    )

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

}
