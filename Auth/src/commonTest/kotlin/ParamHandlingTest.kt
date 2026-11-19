import io.supabase.auth.url.consumeUrlParameter
import kotlin.test.Test
import kotlin.test.assertEquals

class ParamHandlingTest {

    @Test
    fun testConsumeUrlParameterRemoveParameters() {
        val url = "https://example.com/?access_token=token123&refresh_token=refresh123&state=abc&code=xyz"
        val result = consumeUrlParameter(listOf("state", "code"), url)

        assertEquals("https://example.com/?access_token=token123&refresh_token=refresh123", result)
    }

    @Test
    fun testConsumeUrlParameterRemoveAllParameters() {
        val url = "https://example.com/?state=abc&code=xyz"
        val result = consumeUrlParameter(listOf("state", "code"), url)

        assertEquals("https://example.com/", result)
    }

    @Test
    fun testConsumeUrlParameterWithNoMatchingParameters() {
        val url = "https://example.com/?access_token=token123&refresh_token=refresh123"
        val result = consumeUrlParameter(listOf("state", "code"), url)

        assertEquals(url, result)
    }

    @Test
    fun testConsumeUrlParameterWithEmptyParameterList() {
        val url = "https://example.com/?access_token=token123&refresh_token=refresh123"
        val result = consumeUrlParameter(emptyList(), url)

        assertEquals(url, result)
    }

    @Test
    fun testConsumeUrlParameterPreservesFragment() {
        val url = "https://example.com/?access_token=token123&state=abc#fragment"
        val result = consumeUrlParameter(listOf("state"), url)

        assertEquals("https://example.com/?access_token=token123#fragment", result)
    }

    @Test
    fun testConsumeUrlParameterWithComplexUrl() {
        val url = "https://example.com:8080/path/to/resource?access_token=token123&state=abc&expires_in=3600"
        val result = consumeUrlParameter(listOf("state"), url)

        assertEquals("https://example.com:8080/path/to/resource?access_token=token123&expires_in=3600", result)
    }

    @Test
    fun testConsumeUrlParameterWithEncodedValues() {
        val url = "https://example.com/?error=invalid_request&error_description=Invalid+request&state=test"
        val result = consumeUrlParameter(listOf("state"), url)

        assertEquals("https://example.com/?error=invalid_request&error_description=Invalid+request", result)
    }

}
