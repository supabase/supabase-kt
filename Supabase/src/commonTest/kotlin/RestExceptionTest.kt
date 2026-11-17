import io.supabase.exceptions.BadRequestRestException
import io.supabase.exceptions.NotFoundRestException
import io.supabase.exceptions.RestException
import io.supabase.exceptions.UnauthorizedRestException
import io.supabase.exceptions.UnknownRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val REQUEST_URL = "https://projectref.supabase.co/rest/v1/messages"
private const val MASKED_URL = "https://pr.../rest/v1/messages"

class RestExceptionTest {

    private val clients = mutableListOf<HttpClient>()

    @Test
    fun testMessageContainsRequestInformation() = runTest {
        val exception = RestException(
            error = "error",
            description = "description",
            response = mockResponse(httpMethod = HttpMethod.Post)
        )
        val lines = assertNotNull(exception.message).lines()
        assertEquals("error", lines[0])
        assertEquals("description", lines[1])
        assertEquals("URL: $MASKED_URL", lines[2])
        assertTrue(lines[3].startsWith("Headers: "), "Expected a headers line, got '${lines[3]}'")
        assertEquals("Http Method: POST", lines[4])
    }

    @Test
    fun testMessageWithoutDescription() = runTest {
        val exception = RestException(
            error = "error",
            description = null,
            response = mockResponse()
        )
        val lines = assertNotNull(exception.message).lines()
        assertEquals("error", lines[0])
        assertEquals("URL: $MASKED_URL", lines[1])
    }

    @Test
    fun testMessageMasksSensitiveInformation() = runTest {
        val response = mockResponse(
            requestHeaders = mapOf(
                "apikey" to "project-anon-key",
                HttpHeaders.Authorization to "Bearer super-secret-token",
                "X-Client-Info" to "supabase-kt/3.7.0"
            )
        )
        val message = assertNotNull(RestException("error", null, response).message)
        assertFalse(message.contains("projectref"), "The host should be masked")
        assertFalse(message.contains("project-anon-key"), "The api key should be masked")
        assertFalse(message.contains("super-secret-token"), "The access token should be masked")
        assertTrue(message.contains("pr... (len=16)"), "The api key should be masked with its length")
        assertTrue(message.contains("Bearer su... (len=18)"), "The access token should keep its scheme")
        assertTrue(message.contains("supabase-kt/3.7.0"), "Non-sensitive headers should not be masked")
    }

    @Test
    fun testStatusCode() = runTest {
        val exception = RestException("error", null, mockResponse(status = HttpStatusCode.NotFound))
        assertEquals(HttpStatusCode.NotFound.value, exception.statusCode)
    }

    @Test
    fun testUnauthorizedRestException() = runTest {
        val response = mockResponse(status = HttpStatusCode.Unauthorized)
        val exception = UnauthorizedRestException("unauthorized", response, "Invalid api key")
        assertIs<RestException>(exception)
        assertEquals("unauthorized", exception.error)
        assertEquals("Invalid api key", exception.description)
        assertEquals(HttpStatusCode.Unauthorized.value, exception.statusCode)
    }

    @Test
    fun testBadRequestRestException() = runTest {
        val response = mockResponse(status = HttpStatusCode.BadRequest)
        val exception = BadRequestRestException("bad_request", response, "Missing field")
        assertIs<RestException>(exception)
        assertEquals("bad_request", exception.error)
        assertEquals("Missing field", exception.description)
        assertEquals(HttpStatusCode.BadRequest.value, exception.statusCode)
    }

    @Test
    fun testNotFoundRestException() = runTest {
        val response = mockResponse(status = HttpStatusCode.NotFound)
        val exception = NotFoundRestException("not_found", response, "Unknown table")
        assertIs<RestException>(exception)
        assertEquals("not_found", exception.error)
        assertEquals("Unknown table", exception.description)
        assertEquals(HttpStatusCode.NotFound.value, exception.statusCode)
    }

    @Test
    fun testUnknownRestException() = runTest {
        val response = mockResponse(status = HttpStatusCode.InternalServerError)
        val exception = UnknownRestException("unknown", response, "Something went wrong")
        assertIs<RestException>(exception)
        assertEquals("unknown", exception.error)
        assertEquals("Something went wrong", exception.description)
        assertEquals(HttpStatusCode.InternalServerError.value, exception.statusCode)
    }

    @Test
    fun testSubclassesWithoutMessage() = runTest {
        val response = mockResponse()
        assertNull(UnauthorizedRestException("unauthorized", response).description)
        assertNull(BadRequestRestException("bad_request", response).description)
        assertNull(NotFoundRestException("not_found", response).description)
        assertNull(UnknownRestException("unknown", response).description)
    }

    private suspend fun mockResponse(
        status: HttpStatusCode = HttpStatusCode.BadRequest,
        httpMethod: HttpMethod = HttpMethod.Get,
        requestHeaders: Map<String, String> = emptyMap()
    ): HttpResponse {
        val client = HttpClient(MockEngine { respond("", status) })
        clients.add(client)
        return client.request(REQUEST_URL) {
            method = httpMethod
            requestHeaders.forEach { (key, value) -> header(key, value) }
        }
    }

    @AfterTest
    fun cleanup() {
        clients.forEach { it.close() }
        clients.clear()
    }

}
