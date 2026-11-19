import io.supabase.BuildConfig
import io.supabase.OSInformation
import io.supabase.SupabaseClient
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.withMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SupabaseClientTest {

    private lateinit var client: SupabaseClient
    
    @Test
    fun testClientInfoHeader() {
        runTest {
            withMockedSupabaseClient(
                supabaseUrl = "https://example.supabase.co",
                supabaseKey = "somekey",
                requestHandler = {
                    assertEquals(
                        "supabase-kt/${BuildConfig.PROJECT_VERSION}; platform=TestOS; platform-version=1.0.0; runtime=kotlin; runtime-version=${KotlinVersion.CURRENT}",
                        it.headers["X-Client-Info"]
                    )
                    respond("")
                },
                configuration = {
                    osInformation = OSInformation(
                        name = "TestOS",
                        version = "1.0.0",
                    )
                },
                clientHandler = { it.httpClient.get("") }
            )
        }
    }

    @Test
    fun testAccessTokenProvider() {
        runTest {
            client = createMockedSupabaseClient(
                configuration = {
                    accessToken = {
                        "myToken"
                    }
                }
            )
            assertEquals("myToken", client.accessToken?.invoke())
        }
    }

    @Test
    fun testDefaultSerializer() {
        client = createMockedSupabaseClient(
            configuration = {
                defaultSerializer = DummySerializer()
            }
        )
        assertIs<DummySerializer>(client.defaultSerializer, "Default serializer should be an instance of DummySerializer")
    }

    @Test
    fun testClientBuilderParametersWithHttpsUrl() {
        client = createMockedSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "somekey"
        )
        assertEquals("example.supabase.co", client.supabaseUrl, "Supabase url should not contain https://")
        assertEquals("somekey", client.supabaseKey, "Supabase key should be set to somekey")
        assertEquals(
            "https://example.supabase.co",
            client.supabaseHttpUrl,
            "Supabase http url should be https://example.supabase.co"
        )
    }

    @Test
    fun testClientBuilderPlugins() {
        client = createMockedSupabaseClient(
            supabaseUrl = "example.supabase.co",
            supabaseKey = "somekey",
            configuration =  {
                install(TestPlugin) {
                    testValue = true
                }
            }
        )
        val plugin = client.pluginManager.getPluginOrNull(TestPlugin)
        //test if the plugin was correctly installed
        assertNotNull(plugin, "Plugin 'test' should not be null")
        //test if the plugin correctly modified the 'useHTTPS' parameter
        assertEquals(
            "https://example.supabase.co",
            client.supabaseHttpUrl,
            "Supabase http url should be https://example.supabase.co because the plugin modifies it"
        )
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