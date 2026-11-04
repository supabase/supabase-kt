import io.supabase.SupabaseClient
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.resolveAccessToken
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccessTokenTest {

    private lateinit var client: SupabaseClient
    
    @Test
    fun testAccessTokenWithJwtToken() = runTest {
        client = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        client.auth.awaitInitialization()
        client.auth.importAuthToken("myAuth") //this should be ignored as per plugin tokens override the used access token
        assertEquals("myJwtToken", client.resolveAccessToken("myJwtToken"))
    }

    @Test
    fun testAccessTokenWithKeyAsFallback() = runTest {
        client = createMockedSupabaseClient(supabaseKey = "myKey")
        assertEquals("myKey", client.resolveAccessToken())
    }

    @Test
    fun testAccessTokenWithoutKey() = runTest {
        client = createMockedSupabaseClient()
        assertNull(client.resolveAccessToken(keyAsFallback = false))
    }

    @Test
    fun testAccessTokenWithCustomAccessToken() = runTest {
        client = createMockedSupabaseClient(
            configuration = {
                accessToken = {
                    "myCustomToken"
                }
            }
        )
        assertEquals("myCustomToken", client.resolveAccessToken())
    }

    @Test
    fun testAccessTokenWithAuth() = runTest {
        client = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                }
            }
        )
        client.auth.awaitInitialization()
        client.auth.importAuthToken("myAuth")
        assertEquals("myAuth", client.resolveAccessToken())
    }

    @Test
    fun testAccessTokenWithAuthReturnsRefreshedTokenWhenExpired() = runTest {
        val newSession = userSession(customToken = "newToken")
        client = createMockedSupabaseClient(
            configuration = {
                install(Auth) {
                    minimalConfig()
                    alwaysAutoRefresh = true
                }
            }
        ) {
            respondJson(newSession)
        }
        client.auth.awaitInitialization()
        client.auth.importSession(userSession(expiresIn = 0), autoRefresh = false)
        assertEquals("newToken", client.resolveAccessToken())
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