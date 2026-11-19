import io.supabase.StringMasking.maskHeaders
import io.supabase.StringMasking.maskParameterString
import io.supabase.StringMasking.maskParameters
import io.supabase.StringMasking.maskString
import io.supabase.StringMasking.maskUrl
import io.ktor.http.Url
import io.ktor.http.headers
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StringMaskingTest {

    @Test
    fun testEmptyString() {
        assertEquals("", maskString(""))
    }

    @Test
    fun testString() {
        assertEquals("ab...", maskString("abcdefghi"))
    }

    @Test
    fun testStringWithLength() {
        assertEquals("ab... (len=5)", maskString("abcde", showLength = true))
    }

    @Test
    fun testUrl() {
        assertEquals("https://ab.../test?parameter=true", maskUrl(Url("https://abcdefg.supabase.co/test?parameter=true")))
    }

    @Test
    fun testHeaderMasking() {
        val headers = headers {
            set("aa", "bb")
            set("another", "one")
            set("apikey", "areallylongkey")
            set("Authorization", "Bearer thisisasecretkey")
        }
        val maskedString = maskHeaders(headers)
        assertEquals(
            "{aa=[bb], another=[one], apikey=[ar... (len=14)], Authorization=[Bearer th... (len=16)]}",
            maskedString
        )
    }

    /**
     * RFC 9110 section 5.1 makes header field names case-insensitive. A denylist matched against
     * the raw name lets `authorization` or `APIKey` — both valid, and both producible by a user
     * supplied `httpConfig { }` block — through unmasked.
     */
    @Test
    fun testHeaderMaskingIsCaseInsensitive() {
        val headers = headers {
            set("authorization", "Bearer thisisasecretkey")
            set("APIKey", "areallylongkey")
            set("Cookie", "session=supersecretcookie")
        }
        val maskedString = maskHeaders(headers)
        assertFalse(maskedString.contains("thisisasecretkey"), maskedString)
        assertFalse(maskedString.contains("areallylongkey"), maskedString)
        assertFalse(maskedString.contains("supersecretcookie"), maskedString)
    }

    @Test
    fun testHeaderMaskingKeepsBearerPrefixRegardlessOfCase() {
        val masked = maskHeaders(headers { set("authorization", "Bearer thisisasecretkey") })
        assertContains(masked, "Bearer th... (len=16)")
    }

    @Test
    fun testParameterMapMasking() {
        val masked = maskParameters(
            mapOf(
                "access_token" to "secretaccesstoken",
                "refresh_token" to "secretrefreshtoken",
                "expires_in" to "3600",
                "token_type" to "bearer",
            )
        )
        assertFalse(masked.contains("secretaccesstoken"), masked)
        assertFalse(masked.contains("secretrefreshtoken"), masked)
        // Non-sensitive parameters stay readable.
        assertContains(masked, "expires_in=3600")
        assertContains(masked, "token_type=bearer")
    }

    @Test
    fun testRawParameterStringMasking() {
        val masked = maskParameterString(
            "access_token=secretaccesstoken&expires_in=3600&refresh_token=secretrefreshtoken"
        )
        assertFalse(masked.contains("secretaccesstoken"), masked)
        assertFalse(masked.contains("secretrefreshtoken"), masked)
        assertContains(masked, "expires_in=3600")
    }

    @Test
    fun testRawParameterStringPreservesMalformedSegments() {
        assertEquals("notapair", maskParameterString("notapair"))
    }

}
