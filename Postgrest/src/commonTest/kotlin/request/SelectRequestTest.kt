package request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.request.SelectRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SelectRequestTest {

    private lateinit var sut: SelectRequestBuilder

    @Test
    fun testSelectGet() {
        sut = SelectRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            count(Count.EXACT)
        }

        assertEquals("GET", sut.httpMethod.value)
        assertEquals(
            setOf(
                "count=exact"
            ), sut.buildPrefer()
        )
        assertFalse(sut.head)
        assertEquals("EXACT", sut.count.toString())
        assertEquals("public", sut.schema)
    }

    @Test
    fun testSelectHead() {
        sut = SelectRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            count(Count.ESTIMATED)
            head = true
        }

        assertEquals("HEAD", sut.httpMethod.value)
        assertEquals(
            setOf(
                "count=estimated"
            ), sut.buildPrefer()
        )
        assertEquals("ESTIMATED", sut.count.toString())
        assertEquals("public", sut.schema)
    }

}