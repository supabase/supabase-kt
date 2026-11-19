import io.supabase.SupabaseClient
import io.supabase.decode
import io.supabase.encode
import io.supabase.serializer.JacksonSerializer
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonSerializerTest {

    private lateinit var supabaseClient: SupabaseClient
    
    @Test
    fun testJacksonSerializer() {
        val serializer = JacksonSerializer()
        supabaseClient = createMockedSupabaseClient(
            configuration = {
                defaultSerializer = serializer
            }
        )
        assertEquals(serializer, supabaseClient.defaultSerializer)
        val value = mapOf("key" to "value")
        val encoded = serializer.encode(value)
        val decoded = serializer.decode<Map<String, String>>(encoded)
        assertEquals(value, decoded)
    }

    @Test
    fun testJacksonSerializerForNulls() {
        val serializer = JacksonSerializer()
        supabaseClient = createMockedSupabaseClient(
            configuration = {
                defaultSerializer = serializer
            }
        )
        assertEquals(serializer, supabaseClient.defaultSerializer)
        val value = mapOf<String, String?>("key" to null)
        val encoded = serializer.encode(value)
        val decoded = serializer.decode<Map<String, String?>>(encoded)
        assertEquals(value, decoded)
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::supabaseClient.isInitialized) {
                supabaseClient.close()
            }
        }
    }
}