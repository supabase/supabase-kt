import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.event.AuthEvent
import io.supabase.auth.exception.AuthErrorCode
import io.supabase.auth.minimalConfig
import io.supabase.auth.status.SessionStatus
import io.supabase.auth.url.checkForUrlParameterError
import io.supabase.auth.url.handledUrlParameterError
import io.supabase.auth.user.UserSession
import io.supabase.testing.createMockedSupabaseClient
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ErrorHandlingTest {

    private lateinit var supabase: SupabaseClient
    
    // Tests for checkForUrlParameterError
    @Test
    fun testCheckForUrlParameterErrorWithAllParameters() {
        val url =
            Url("https://example.com/?error=invalid_request&error_code=otp_expired&error_description=Invalid+request")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("otp_expired", error.error)
        assertEquals("Invalid request (invalid_request)", error.errorDescription)
        assertEquals(AuthErrorCode.OtpExpired, error.errorCode)
    }

    @Test
    fun testCheckForUrlParameterErrorWithNoError() {
        val url = Url("https://example.com/?access_token=token123")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNull(error)
    }

    @Test
    fun testCheckForUrlParameterErrorWithOnlyErrorCode() {
        val url = Url("https://example.com/?error_code=otp_expired")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("otp_expired", error.error)
        assertEquals("otp_expired", error.errorDescription)
    }

    @Test
    fun testCheckForUrlParameterErrorWithOnlyError() {
        val url = Url("https://example.com/?error=access_denied")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("access_denied", error.error)
        assertEquals("access_denied", error.errorDescription)
        assertNull(error.errorCode)
    }

    @Test
    fun testCheckForUrlParameterErrorWithOnlyErrorDescription() {
        val url = Url("https://example.com/?error_description=Something+went+wrong")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("Something went wrong", error.error)
        assertEquals("Something went wrong", error.errorDescription)
    }

    @Test
    fun testCheckForUrlParameterErrorWithErrorCodeAndDescription() {
        val url = Url("https://example.com/?error_code=otp_expired&error_description=Invalid+request")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("otp_expired", error.error)
        assertEquals("Invalid request", error.errorDescription)
    }

    @Test
    fun testCheckForUrlParameterErrorWithMissingErrorCode() {
        val url = Url("https://example.com/?error=invalid_request&error_description=Something+went+wrong")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("invalid_request", error.error)
        assertEquals("Something went wrong (invalid_request)", error.errorDescription)
        assertNull(error.errorCode)
    }

    @Test
    fun testCheckForUrlParameterErrorWithDifferentErrorCode() {
        val url =
            Url("https://example.com/?error=bad_request&error_code=validation_failed&error_description=Validation+error")
        val error = checkForUrlParameterError { url.parameters[it] }

        assertNotNull(error)
        assertEquals("validation_failed", error.error)
        assertEquals("Validation error (bad_request)", error.errorDescription)
    }

    // Tests for handledUrlParameterError
    @Test
    fun testHandledUrlParameterErrorWithValidError() = runTest {
        val url =
            Url("https://example.com/?error=invalid_request&error_code=otp_expired&error_description=Invalid+request")
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        supabase.auth.awaitInitialization()
        val handled = supabase.auth.handledUrlParameterError {
            url.parameters[it]
        }

        assertTrue { handled }
        assertIs<SessionStatus.NotAuthenticated>(supabase.auth.sessionStatus.value)
        supabase.auth.events.test {
            val event = expectMostRecentItem()
            assertIs<AuthEvent.OtpError>(event)
            assertEquals(AuthErrorCode.OtpExpired, event.errorCode)
            assertEquals("Invalid request (invalid_request)", event.errorDescription)
        }
    }

    @Test
    fun testHandledUrlParameterErrorWithNoError() = runTest {
        val url = Url("https://example.com/")
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )

        supabase.auth.awaitInitialization()
        val handled = supabase.auth.handledUrlParameterError {
            url.parameters[it]
        }

        assertFalse { handled }
        assertIs<SessionStatus.NotAuthenticated>(supabase.auth.sessionStatus.value)
        supabase.auth.events.test(timeout = 1.seconds) {
            expectNoEvents()
        }
    }

    @Test
    fun testHandledUrlParameterErrorWithEmptyParameters() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )

        val handled = supabase.auth.handledUrlParameterError { null }

        assertFalse { handled }
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

private fun userSession(customToken: String = "accessToken", expiresIn: Long = 3600) = UserSession(
    accessToken = customToken,
    refreshToken = "refreshToken",
    expiresIn = expiresIn,
    tokenType = "Bearer",
    user = null
)
