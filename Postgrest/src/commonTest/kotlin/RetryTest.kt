import io.supabase.SupabaseClientBuilder
import io.supabase.postgrest.Postgrest
import io.supabase.postgrest.from
import io.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class RetryTest {

    private var requestCount = 0

    private fun configureClient(maxRetries: Int = 3): SupabaseClientBuilder.() -> Unit = {
        install(Postgrest) {
            this.maxRetries = maxRetries
        }
    }

    @Test
    fun testRetryOnHttp503() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            if (requestCount < 3) {
                respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond("[]")
            }
        }
        val result = supabase.from("test").select()
        assertEquals(3, requestCount, "Should have retried until success")
    }

    @Test
    fun testRetryOnHttp520() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            if (requestCount < 2) {
                respond("Unknown Error", HttpStatusCode(520, "Unknown Error"))
            } else {
                respond("[]")
            }
        }
        val result = supabase.from("test").select()
        assertEquals(2, requestCount, "Should have retried once then succeeded")
    }

    @Test
    fun testNoRetryForPost() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
        }
        // POST (insert) should not retry - it will get a RestException from the error response
        assertFails {
            supabase.from("test").insert(buildJsonArray {
                add(buildJsonObject { put("id", 1) })
            })
        }
        assertEquals(1, requestCount, "POST should not retry")
    }

    @Test
    fun testNoRetryWhenDisabled() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
        }
        assertFails {
            supabase.from("test").select {
                noRetry()
            }
        }
        assertEquals(1, requestCount, "Should not retry when noRetry() is set")
    }

    @Test
    fun testMaxRetriesExhausted() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient(maxRetries = 2)
        ) {
            requestCount++
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
        }
        // All retries fail with 503 - the last response should be returned as a result
        // (the error parsing happens in asPostgrestResult, not in the retry logic)
        assertFails {
            supabase.from("test").select()
        }
        assertEquals(3, requestCount, "Should attempt 1 + 2 retries = 3 total requests")
    }

    @Test
    fun testRetryCountHeader() = runTest {
        requestCount = 0
        var lastRetryHeader: String? = null
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            lastRetryHeader = it.headers["x-retry-count"]
            if (requestCount < 3) {
                respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond("[]")
            }
        }
        supabase.from("test").select()
        assertEquals("2", lastRetryHeader, "x-retry-count should be the attempt number")
    }

    @Test
    fun testNoRetryHeaderOnFirstRequest() = runTest {
        requestCount = 0
        var firstRetryHeader: String? = "not-checked"
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            if (requestCount == 1) {
                firstRetryHeader = it.headers["x-retry-count"]
            }
            respond("[]")
        }
        supabase.from("test").select()
        assertEquals(null, firstRetryHeader, "First request should not have x-retry-count header")
    }

    @Test
    fun testZeroMaxRetriesDisablesRetry() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient(maxRetries = 0)
        ) {
            requestCount++
            respond("Service Unavailable", HttpStatusCode.ServiceUnavailable)
        }
        assertFails {
            supabase.from("test").select()
        }
        assertEquals(1, requestCount, "Should not retry when maxRetries is 0")
    }

    @Test
    fun testRetryOnNetworkError() = runTest {
        requestCount = 0
        val supabase = createMockedSupabaseClient(
            configuration = configureClient()
        ) {
            requestCount++
            if (requestCount < 2) {
                throw RuntimeException("Connection reset")
            }
            respond("[]")
        }
        val result = supabase.from("test").select()
        assertEquals(2, requestCount, "Should retry on network error")
    }
}
