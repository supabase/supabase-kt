package io.supabase.integration

import io.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FunctionsIntegrationTest : IntegrationTestBase() {

    @Test
    fun `invoke echo function returns the same body`() = runTest {
        val client = createAuthenticatedClient()
        val requestBody = buildJsonObject {
            put("message", "hello")
        }
        val response = client.functions.invoke(
            function = "echo",
            body = requestBody,
            headers = headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        )
        assertEquals("""{"message":"hello"}""", response.bodyAsText())
    }

    @Test
    fun `invokeSSE receives server-sent events`() = runTest {
        val client = createAuthenticatedClient()
        val events = client.functions.invokeSSE("sse").toList()
        val dataValues = events.map { it.data }
        assertEquals(listOf("hello", "world", "done"), dataValues)
    }
}
