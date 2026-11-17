import io.supabase.SupabaseClient
import io.supabase.decode
import io.supabase.encode
import io.supabase.serializer.MoshiSerializer
import io.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals

class MoshiSerializerTest {

    private lateinit var supabaseClient: SupabaseClient
    
    @Test
    fun testMoshiSerializer() {
        val serializer = MoshiSerializer()
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
    fun testMoshiSerializerForNulls() {
        val serializer = MoshiSerializer()
        supabaseClient = createMockedSupabaseClient(
            configuration = {
                defaultSerializer = serializer
            })
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