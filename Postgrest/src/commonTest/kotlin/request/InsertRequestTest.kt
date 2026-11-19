package request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.request.InsertRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertRequestTest {

    private lateinit var sut: InsertRequestBuilder

    @Test
    fun testInsertRequestBuilder() {
        sut = InsertRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            defaultToNull = false
            count(Count.EXACT)
            select()
        }

        assertEquals("POST", sut.httpMethod.value)
        assertEquals("public", sut.schema)
        assertEquals(
            setOf(
                "return=representation",
                "missing=default",
                "count=exact"
            ), sut.buildPrefer()
        )
    }

    @Test
    fun testInsertRequestBuilderDefault() {
        sut = InsertRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            defaultToNull = true
            count(Count.EXACT)
            select()
        }

        assertEquals("POST", sut.httpMethod.value)
        assertEquals("public", sut.schema)
        assertEquals(
            setOf(
                "return=representation",
                "count=exact"
            ), sut.buildPrefer()
        )
    }

}