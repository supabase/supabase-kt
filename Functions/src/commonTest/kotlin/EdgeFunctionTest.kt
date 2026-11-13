import io.supabase.SupabaseClient
import io.supabase.functions.FunctionRegion
import io.supabase.functions.functions
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.pathAfterVersion
import io.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeFunctionTest {

    private lateinit var supabase: SupabaseClient

    @Test
    fun testInvokingWithoutBody() {
        runTest {
            val expectedName = "myFunction"
            val expectedRegion = FunctionRegion.EU_WEST_1
            val expectedHeaders = Headers.build {
                append("myHeader", "myValue")
            }
            supabase = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    assertEquals("POST", it.method.value)
                    assertEquals("/$expectedName", it.url.pathAfterVersion())
                    assertEquals(expectedRegion.value, it.headers["x-region"])
                    assertEquals("myValue", it.headers["myHeader"])
                    respond("")
                }
            )
            val function = supabase.functions.buildEdgeFunction(expectedName, expectedRegion, expectedHeaders)
            function()
        }
    }

    @Test
    fun testInvokingWithBody() {
        runTest {
            val expectedName = "myFunction"
            val expectedRegion = FunctionRegion.EU_WEST_1
            val expectedHeaders = Headers.build {
                set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            val expectedBody = buildJsonObject {
                put("key", "value")
            }
            supabase = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    val body = it.body.toJsonElement().jsonObject
                    assertEquals(expectedBody, body)
                    assertEquals("POST", it.method.value)
                    assertEquals("/$expectedName", it.url.pathAfterVersion())
                    assertEquals(expectedRegion.value, it.headers["x-region"])
                    respond("")
                }
            )
            val function = supabase.functions.buildEdgeFunction(expectedName, expectedRegion, expectedHeaders)
            function(expectedBody)
        }
    }

    // invokeSSE() cannot be unit-tested with MockEngine because it lacks SSECapability.
    // SSE streaming is covered by FunctionsIntegrationTest instead.

    @Test
    fun testPrepareInvoke() {
        runTest {
            val expectedName = "myFunction"
            val expectedRegion = FunctionRegion.EU_WEST_1
            supabase = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    assertEquals("POST", it.method.value)
                    assertEquals("/$expectedName", it.url.pathAfterVersion())
                    assertEquals(expectedRegion.value, it.headers["x-region"])
                    respond("streaming content")
                }
            )
            val function = supabase.functions.buildEdgeFunction(expectedName, expectedRegion)
            val statement = function.prepareInvoke()
            statement.execute { response ->
                assertEquals("streaming content", response.bodyAsText())
            }
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::supabase.isInitialized) {
                supabase.close()
            }
        }
    }

}
