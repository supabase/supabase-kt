import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.exception.AuthErrorCode
import io.supabase.auth.exception.AuthRestException
import io.supabase.auth.exception.AuthSessionMissingException
import io.supabase.auth.exception.AuthWeakPasswordException
import io.supabase.auth.minimalConfig
import io.supabase.auth.providers.builtin.Email
import io.supabase.exceptions.BadRequestRestException
import io.supabase.exceptions.UnauthorizedRestException
import io.supabase.exceptions.UnknownRestException
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.respondJson
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNull

class AuthRestExceptionTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }
    
    private lateinit var client: SupabaseClient

    @Test
    fun testErrorsWithErrorCode() {
        runTest {
            client = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    respondJson(
                        code = HttpStatusCode.BadRequest,
                        data = buildJsonObject {
                            put("error_code", "error_code")
                            put("message", "error_message")
                        }
                    )
                }
            )
            val exception = assertFailsWith<AuthRestException> {
                client.signUp()
            }
            assertEquals("error_code", exception.error)
            assertEquals("error_message", exception.errorDescription)
        }
    }

    @Test
    fun testPasswordWeakAuthRestException() {
        runTest {
            client = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    respondJson(
                        code = HttpStatusCode.BadRequest,
                        data = buildJsonObject {
                            put("error_code", "weak_password")
                            put("message", "error_message")
                            put("weak_password", buildJsonObject {
                                putJsonArray("reasons") {
                                    add("reason1")
                                    add("reason2")
                                }
                            })
                        }
                    )
                }
            )
            val exception = assertFailsWith<AuthWeakPasswordException> {
                client.signUp()
            }
            assertEquals("weak_password", exception.error)
            assertEquals("error_message", exception.errorDescription)
            assertEquals(listOf("reason1", "reason2"), exception.reasons)
        }
    }

    @Test
    fun testErrorsWithoutErrorCode() {
        runTest {
            client = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    respondJson(
                        code = HttpStatusCode.BadRequest,
                        data = buildJsonObject {
                            put("message", "error_message")
                        }
                    )
                }
            )
            val exception = assertFails {
                client.signUp()
            }
            assertIsNot<AuthRestException>(exception)
            assertIs<BadRequestRestException>(exception)
        }
    }

    @Test
    fun testErrorsWithNonObjectBody() {
        runTest {
            client = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    respondError(HttpStatusCode.BadRequest, "maintenance")
                }
            )
            val exception = assertFails {
                client.signUp()
            }
            assertIsNot<AuthRestException>(exception)
            assertIs<BadRequestRestException>(exception)
        }
    }

    @Test
    fun testKnownErrorCodeIsMappedToAuthErrorCode() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.BadRequest,
                body = buildJsonObject {
                    put("error_code", "invalid_credentials")
                    put("message", "Invalid login credentials")
                }
            )
            val exception = assertFailsWith<AuthRestException> {
                client.signUp()
            }
            assertEquals(AuthErrorCode.InvalidCredentials, exception.errorCode)
            assertEquals("invalid_credentials", exception.error)
            assertEquals("Invalid login credentials", exception.errorDescription)
            assertEquals(HttpStatusCode.BadRequest.value, exception.statusCode)
        }
    }

    @Test
    fun testUnknownErrorCodeHasNoAuthErrorCode() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.BadRequest,
                body = buildJsonObject {
                    put("error_code", "not_a_known_error_code")
                    put("message", "error_message")
                }
            )
            val exception = assertFailsWith<AuthRestException> {
                client.signUp()
            }
            assertNull(exception.errorCode, "Unknown error codes should not be mapped to an AuthErrorCode")
            assertEquals("not_a_known_error_code", exception.error)
        }
    }

    @Test
    fun testSessionNotFoundAuthRestException() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.BadRequest,
                body = buildJsonObject {
                    put("error_code", "session_not_found")
                    put("message", "error_message")
                }
            )
            val exception = assertFailsWith<AuthSessionMissingException> {
                client.signUp()
            }
            assertEquals(AuthErrorCode.SessionNotFound, exception.errorCode)
            assertEquals("session_not_found", exception.error)
        }
    }

    @Test
    fun testUnauthorizedErrorWithoutErrorCode() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.Unauthorized,
                body = buildJsonObject {
                    put("message", "error_message")
                }
            )
            val exception = assertFailsWith<UnauthorizedRestException> {
                client.signUp()
            }
            assertEquals("Unauthorized", exception.error)
            assertEquals("error_message", exception.description)
        }
    }

    @Test
    fun testUnprocessableEntityErrorWithoutErrorCode() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.UnprocessableEntity,
                body = buildJsonObject {
                    put("message", "error_message")
                }
            )
            val exception = assertFailsWith<BadRequestRestException> {
                client.signUp()
            }
            assertEquals("Unprocessable Entity", exception.error)
            assertEquals("error_message", exception.description)
        }
    }

    @Test
    fun testOtherStatusCodesWithoutErrorCodeAreUnknown() {
        runTest {
            client = clientRespondingWith(
                code = HttpStatusCode.InternalServerError,
                body = buildJsonObject {
                    put("message", "error_message")
                }
            )
            val exception = assertFailsWith<UnknownRestException> {
                client.signUp()
            }
            assertEquals("Unknown Error", exception.error)
            assertNull(exception.description)
        }
    }

    private fun clientRespondingWith(code: HttpStatusCode, body: JsonObject) = createMockedSupabaseClient(
        configuration = configuration,
        requestHandler = {
            respondJson(code = code, data = body)
        }
    )

    private suspend fun SupabaseClient.signUp() = auth.signUpWith(Email) {
        email = "example@email.com"
        password = "password"
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

}