import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.event.AuthEvent
import io.supabase.auth.minimalConfig
import io.supabase.auth.url.UrlValidationResult
import io.supabase.auth.url.consumeHashParameters
import io.supabase.auth.url.getFragmentParts
import io.supabase.auth.url.validateHash
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HashParsingTest {

    private lateinit var supabase: SupabaseClient
    
    // Tests for validateHash
    @Test
    fun testValidateHashWithValidSession() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&refresh_token=refresh_token&expires_in=3600&token_type=Bearer"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.SessionFound>(result)
        assertEquals("test_token", result.session.accessToken)
        assertEquals("refresh_token", result.session.refreshToken)
        assertEquals(3600L, result.session.expiresIn)
        assertEquals("Bearer", result.session.tokenType)
    }

    @Test
    fun testValidateHashWithValidSessionAndProviderTokens() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash =
            "access_token=test_token&refresh_token=refresh_token&expires_in=3600&token_type=Bearer&provider_token=provider_token_123&provider_refresh_token=provider_refresh_123"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.SessionFound>(result)
        assertEquals("test_token", result.session.accessToken)
        assertEquals("provider_token_123", result.session.providerToken)
        assertEquals("provider_refresh_123", result.session.providerRefreshToken)
    }

    @Test
    fun testValidateHashWithError() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "error=invalid_request&error_code=otp_expired&error_description=Invalid+request"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.ErrorFound>(result)
    }

    @Test
    fun testValidateHashWithEncodedErrorEmitsDecodedDescription() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        supabase.auth.awaitInitialization()
        val hash = "error=access_denied&error_code=otp_expired&error_description=Email+link+is+invalid+or+has+expired"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.ErrorFound>(result)
        supabase.auth.events.test {
            val event = expectMostRecentItem()
            assertIs<AuthEvent.OtpError>(event)
            assertEquals("otp_expired", event.error)
            assertEquals("Email link is invalid or has expired (access_denied)", event.errorDescription)
        }
    }

    @Test
    fun testValidateHashWithErrorWithoutErrorCode() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        supabase.auth.awaitInitialization()
        val hash = "error=access_denied&error_description=The+user+denied+the+request"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.ErrorFound>(result)
        supabase.auth.events.test {
            val event = expectMostRecentItem()
            assertIs<AuthEvent.OtpError>(event)
            assertEquals("access_denied", event.error)
            assertEquals("The user denied the request (access_denied)", event.errorDescription)
        }
    }

    @Test
    fun testValidateHashWithMissingAccessToken() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "refresh_token=refresh_token&expires_in=3600&token_type=Bearer"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithMissingRefreshToken() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&expires_in=3600&token_type=Bearer"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithMissingExpiresIn() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&refresh_token=refresh_token&token_type=Bearer"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithMissingTokenType() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&refresh_token=refresh_token&expires_in=3600"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithInvalidExpiresIn() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&refresh_token=refresh_token&expires_in=invalid&token_type=Bearer"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithEmptyString() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = ""
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.Skipped>(result)
    }

    @Test
    fun testValidateHashWithTypeParameter() = runTest {
        supabase = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        val hash = "access_token=test_token&refresh_token=refresh_token&expires_in=3600&token_type=Bearer&type=recovery"
        val result = supabase.auth.validateHash(hash)

        assertIs<UrlValidationResult.SessionFound>(result)
        assertEquals("recovery", result.session.type)
    }

    // Tests for getFragmentParts
    @Test
    fun testGetFragmentPartsWithMultipleParameters() {
        val fragment = "access_token=token123&refresh_token=refresh123&expires_in=3600&token_type=Bearer"
        val parts = getFragmentParts(fragment)

        assertEquals(4, parts.size)
        assertEquals("token123", parts["access_token"])
        assertEquals("refresh123", parts["refresh_token"])
        assertEquals("3600", parts["expires_in"])
        assertEquals("Bearer", parts["token_type"])
    }

    @Test
    fun testGetFragmentPartsWithSingleParameter() {
        val fragment = "access_token=token123"
        val parts = getFragmentParts(fragment)

        assertEquals(1, parts.size)
        assertEquals("token123", parts["access_token"])
    }

    @Test
    fun testGetFragmentPartsWithEmptyString() {
        val fragment = ""
        val parts = getFragmentParts(fragment)

        assertEquals(0, parts.size)
    }

    @Test
    fun testGetFragmentPartsWithEncodedValues() {
        val fragment = "error=invalid_request&error_code=otp_expired&error_description=Invalid+request"
        val parts = getFragmentParts(fragment)

        assertEquals(3, parts.size)
        assertEquals("invalid_request", parts["error"])
        assertEquals("otp_expired", parts["error_code"])
        assertEquals("Invalid request", parts["error_description"])
    }

    @Test
    fun testGetFragmentPartsWithSpecialCharacters() {
        val fragment = "param1=value%201&param2=value%3D2&param3=a%26b"
        val parts = getFragmentParts(fragment)

        assertEquals(3, parts.size)
        assertEquals("value 1", parts["param1"])
        assertEquals("value=2", parts["param2"])
        assertEquals("a&b", parts["param3"])
    }

    @Test
    fun testGetFragmentPartsWithMalformedEscapeKeepsRawValues() {
        val fragment = "access_token=token%zz&refresh_token=refresh123"
        val parts = getFragmentParts(fragment)

        assertEquals(2, parts.size)
        assertEquals("token%zz", parts["access_token"])
        assertEquals("refresh123", parts["refresh_token"])
    }

    @Test
    fun testGetFragmentPartsWithValuelessKey() {
        val fragment = "param1&param2=value2"
        val parts = getFragmentParts(fragment)

        assertEquals(2, parts.size)
        assertEquals("", parts["param1"])
        assertEquals("value2", parts["param2"])
    }

    @Test
    fun testGetFragmentPartsWithEmptyValues() {
        val fragment = "param1=&param2=value2&param3="
        val parts = getFragmentParts(fragment)

        assertEquals(3, parts.size)
        assertEquals("", parts["param1"])
        assertEquals("value2", parts["param2"])
        assertEquals("", parts["param3"])
    }

    // Tests for consumeHashParameters
    @Test
    fun testConsumeHashParametersRemoveSingleParameter() {
        val url = "https://example.com/#access_token=token123&refresh_token=refresh123&state=abc123"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersRemoveMultipleParameters() {
        val url = "https://example.com/#access_token=token123&refresh_token=refresh123&state=abc&code=xyz"
        val result = consumeHashParameters(listOf("state", "code"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersRemoveAllParameters() {
        val url = "https://example.com/#state=abc&code=xyz"
        val result = consumeHashParameters(listOf("state", "code"), url)
        val expectedUrl = "https://example.com/"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersWithNoMatchingParameters() {
        val url = "https://example.com/#access_token=token123&refresh_token=refresh123"
        val result = consumeHashParameters(listOf("state", "code"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersWithEmptyParameterList() {
        val url = "https://example.com/#access_token=token123&refresh_token=refresh123"
        val result = consumeHashParameters(emptyList(), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersWithNoFragment() {
        val url = "https://example.com/"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersWithEmptyFragment() {
        val url = "https://example.com/#"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersPreservesQueryParameters() {
        val url = "https://example.com/?query=value#access_token=token123&state=abc"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/?query=value#access_token=token123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersRemoveFirstParameter() {
        val url = "https://example.com/#state=abc&access_token=token123&refresh_token=refresh123"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersRemoveMiddleParameter() {
        val url = "https://example.com/#access_token=token123&state=abc&refresh_token=refresh123"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersRemoveLastParameter() {
        val url = "https://example.com/#access_token=token123&refresh_token=refresh123&state=abc"
        val result = consumeHashParameters(listOf("state"), url)
        val expectedUrl = "https://example.com/#access_token=token123&refresh_token=refresh123"

        assertEquals(expectedUrl, result)
    }

    @Test
    fun testConsumeHashParametersWithComplexUrl() {
        val url =
            "https://example.com:8080/path/to/resource?query1=value1&query2=value2#access_token=token123&state=abc&code=xyz&expires_in=3600"
        val result = consumeHashParameters(listOf("state", "code"), url)
        val expectedUrl =
            "https://example.com:8080/path/to/resource?query1=value1&query2=value2#access_token=token123&expires_in=3600"

        assertEquals(expectedUrl, result)
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
