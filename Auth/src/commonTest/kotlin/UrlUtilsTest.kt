import io.supabase.auth.redirectTo
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlUtilsTest {

    @Test
    fun testRedirectTo() {
        val url = "https://example.com/"
        val redirectTo = "https://redirect.com"
        val newUrl = HttpRequestBuilder().apply {
            url(url)
            redirectTo(redirectTo)
        }.url.toString()
        val expectedUrl = "https://example.com/?redirect_to=https%3A%2F%2Fredirect.com"
        assertEquals(expectedUrl, newUrl)
    }

}