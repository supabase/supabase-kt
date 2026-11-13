import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.ktor.http.Headers
import io.ktor.http.headersOf
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgrestResultTest {

    private val supabase = createMockedSupabaseClient(configuration = {
        install(Postgrest)
    })

    @Serializable
    data class TestData(val id: Int, val name: String)

    @Test
    fun testCountOrNullWithCount() {
        val headers = headersOf("Content-Range", "0-14/15")
        val result = PostgrestResult("[]", headers, supabase.postgrest)
        assertEquals(15L, result.countOrNull())
    }

    @Test
    fun testCountOrNullWithoutCount() {
        val headers = headersOf("Content-Range", "0-14/*")
        val result = PostgrestResult("[]", headers, supabase.postgrest)
        assertNull(result.countOrNull())
    }

    @Test
    fun testCountOrNullMissingHeader() {
        val result = PostgrestResult("[]", Headers.Empty, supabase.postgrest)
        assertNull(result.countOrNull())
    }

    @Test
    fun testRangeOrNull() {
        val headers = headersOf("Content-Range", "0-14/15")
        val result = PostgrestResult("[]", headers, supabase.postgrest)
        assertEquals(0L..14L, result.rangeOrNull())
    }

    @Test
    fun testRangeOrNullEmptyResult() {
        listOf("*/0", "*/*").forEach { contentRange ->
            val result = PostgrestResult("[]", headersOf("Content-Range", contentRange), supabase.postgrest)
            assertNull(result.rangeOrNull())
        }
    }

    @Test
    fun testRangeOrNullMissingHeader() {
        val result = PostgrestResult("[]", Headers.Empty, supabase.postgrest)
        assertNull(result.rangeOrNull())
    }

    @Test
    fun testDecodeAs() {
        val json = """{"id": 1, "name": "Item"}"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        val decoded = result.decodeAs<TestData>()
        assertEquals(1, decoded.id)
        assertEquals("Item", decoded.name)
    }

    @Test
    fun testDecodeAsOrNullValid() {
        val json = """{"id": 1, "name": "Item"}"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        val decoded = result.decodeAsOrNull<TestData>()
        assertEquals(1, decoded?.id)
    }

    @Test
    fun testDecodeAsOrNullInvalid() {
        val json = """{"invalid": data}"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        assertNull(result.decodeAsOrNull<TestData>())
    }

    @Test
    fun testDecodeList() {
        val json = """[{"id": 1, "name": "Item1"}, {"id": 2, "name": "Item2"}]"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        val decoded = result.decodeList<TestData>()
        assertEquals(2, decoded.size)
        assertEquals(1, decoded[0].id)
        assertEquals(2, decoded[1].id)
    }

    @Test
    fun testDecodeSingle() {
        val json = """[{"id": 1, "name": "Item1"}]"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        val decoded = result.decodeSingle<TestData>()
        assertEquals(1, decoded.id)
    }

    @Test
    fun testDecodeSingleOrNullValid() {
        val json = """[{"id": 1, "name": "Item1"}]"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        val decoded = result.decodeSingleOrNull<TestData>()
        assertEquals(1, decoded?.id)
    }

    @Test
    fun testDecodeSingleOrNullEmpty() {
        val json = """[]"""
        val result = PostgrestResult(json, Headers.Empty, supabase.postgrest)
        assertNull(result.decodeSingleOrNull<TestData>())
    }

    @Test
    fun testComponents() {
        val headers = headersOf("Content-Range", "0-14/15")
        val json = "[]"
        val result = PostgrestResult(json, headers, supabase.postgrest)
        val (dataComponent, headersComponent) = result
        assertEquals(json, dataComponent)
        assertEquals(headers, headersComponent)
    }
}
