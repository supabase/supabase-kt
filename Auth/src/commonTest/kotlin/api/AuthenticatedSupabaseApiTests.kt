package api

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.auth.api.ResolveAccessToken
import io.supabase.auth.api.buildAuthConfig
import io.supabase.auth.exception.SessionRequiredException
import io.supabase.testing.MockedHttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthenticatedSupabaseApiTests {

    @Test
    fun testRequestWithBearerToken() = runTest {
        val expectedToken = "test-access-token"
        val expectedUrl = "test.url.de/"

        val httpClient = MockedHttpClient {
            assertEquals("Bearer $expectedToken", it.headers["Authorization"])
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { expectedUrl + it }
                requireSession = true
                getAccessToken = ResolveAccessToken { _, _ -> expectedToken }
            }
        )

        api.get("test")
    }

    @Test
    fun testRequestWithoutSessionThrowsException() = runTest {
        val httpClient = MockedHttpClient {
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { "test.url.de/$it" }
                requireSession = true
                getAccessToken = ResolveAccessToken { _, _ -> null }
            }
        )

        assertFailsWith<SessionRequiredException> {
            api.get("test")
        }
    }

    @Test
    fun testRequestWithNewApiKeyFail() = runTest {
        val httpClient = MockedHttpClient {
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { "test.url.de/$it" }
                requireSession = true
                useNewApiKeyAsFallback = false
                getAccessToken = ResolveAccessToken { _, _ -> "sb_publishable_" }
            }
        )

        assertFailsWith<SessionRequiredException> {
            api.get("test")
        }
    }


    @Test
    fun testRequestWithoutRequiredSessionUsesFallbackResolverPath() = runTest {
        val fallbackToken = "fallback-token"

        val httpClient = MockedHttpClient {
            assertEquals("Bearer $fallbackToken", it.headers["Authorization"])
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { "test.url.de/$it" }
                requireSession = false
                getAccessToken = ResolveAccessToken { _, fallbackToKey ->
                    if (fallbackToKey) fallbackToken else null
                }
            }
        )

        api.get("test")
    }

    @Test
    fun testPrepareRequest() = runTest {
        val expectedToken = "test-access-token"

        val httpClient = MockedHttpClient {
            assertEquals("Bearer $expectedToken", it.headers["Authorization"])
            assertEquals(HttpMethod.Post, it.method)
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { "test.url.de/$it" }
                requireSession = true
                getAccessToken = ResolveAccessToken { _, _ -> expectedToken }
            }
        )

        val statement = api.prepareRequest("test") {
            method = HttpMethod.Post
        }
        statement.execute()
    }

    @Test
    fun testRawRequestWithoutPath() = runTest {
        val expectedToken = "test-access-token"
        val baseUrl = "https://api.example.com"
        var requestUrl = ""

        val httpClient = MockedHttpClient {
            requestUrl = it.url.toString()
            respond("")
        }

        val api = AuthenticatedSupabaseApi(
            httpClient = httpClient,
            config = buildAuthConfig {
                resolveUrl = { "$baseUrl/$it" }
                requireSession = true
                getAccessToken = ResolveAccessToken { _, _ -> expectedToken }
            }
        )

        api.get("")

        assertTrue(requestUrl.contains(baseUrl))
    }
}
