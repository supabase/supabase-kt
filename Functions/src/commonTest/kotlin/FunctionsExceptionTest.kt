import io.supabase.SupabaseClient
import io.supabase.exceptions.BadRequestRestException
import io.supabase.exceptions.NotFoundRestException
import io.supabase.exceptions.RestException
import io.supabase.exceptions.UnauthorizedRestException
import io.supabase.exceptions.UnknownRestException
import io.supabase.functions.Functions
import io.supabase.functions.functions
import io.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val ERROR_BODY = "Missing authorization header"

class FunctionsExceptionTest {

    private lateinit var supabase: SupabaseClient

    @Test
    fun testUnauthorizedError() = runTest {
        mockErrorResponse(HttpStatusCode.Unauthorized)
        val exception = assertFailsWith<UnauthorizedRestException> {
            supabase.functions.invoke(function = "function")
        }
        assertEquals(ERROR_BODY, exception.error)
        assertEquals(HttpStatusCode.Unauthorized.value, exception.statusCode)
    }

    @Test
    fun testNotFoundError() = runTest {
        mockErrorResponse(HttpStatusCode.NotFound)
        val exception = assertFailsWith<NotFoundRestException> {
            supabase.functions.invoke(function = "function")
        }
        assertEquals(ERROR_BODY, exception.error)
        assertEquals(HttpStatusCode.NotFound.value, exception.statusCode)
    }

    @Test
    fun testBadRequestError() = runTest {
        mockErrorResponse(HttpStatusCode.BadRequest)
        val exception = assertFailsWith<BadRequestRestException> {
            supabase.functions.invoke(function = "function")
        }
        assertEquals(ERROR_BODY, exception.error)
        assertEquals(HttpStatusCode.BadRequest.value, exception.statusCode)
    }

    @Test
    fun testOtherErrorsAreUnknown() = runTest {
        mockErrorResponse(HttpStatusCode.InternalServerError)
        val exception = assertFailsWith<UnknownRestException> {
            supabase.functions.invoke(function = "function")
        }
        assertEquals(ERROR_BODY, exception.error)
        assertEquals(HttpStatusCode.InternalServerError.value, exception.statusCode)
    }

    @Test
    fun testErrorUsesRawResponseBody() = runTest {
        //the functions plugin does not parse the error body, it uses the raw body as the error
        mockErrorResponse(HttpStatusCode.BadRequest, """{"error":"boom"}""")
        val exception = assertFailsWith<RestException> {
            supabase.functions.invoke(function = "function")
        }
        assertEquals("""{"error":"boom"}""", exception.error)
        assertNull(exception.description)
    }

    private fun mockErrorResponse(status: HttpStatusCode, body: String = ERROR_BODY) {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Functions)
            },
            requestHandler = {
                respond(body, status)
            }
        )
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
