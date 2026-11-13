package api

import io.supabase.auth.api.ResolveAccessToken
import io.supabase.auth.api.buildAuthConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthenticatedApiConfigBuilderTests {

    @Test
    fun testBuildAuthConfigSetsGroupedValues() = runTest {
        val config = buildAuthConfig {
            resolveUrl = { "https://api.example.com/$it" }
            parseErrorResponse = null
            jwtToken = "jwt-token"
            defaultRequest = null
            requireSession = true
            urlLengthLimit = 123
            getAccessToken = ResolveAccessToken { token, _ -> token }
        }

        assertEquals("https://api.example.com/test", config.context.resolveUrl("test"))
        assertNull(config.context.parseErrorResponse)
        assertEquals("jwt-token", config.auth.jwtToken)
        assertEquals(true, config.auth.requireSession)
        assertEquals(123, config.request.urlLengthLimit)
    }

}
