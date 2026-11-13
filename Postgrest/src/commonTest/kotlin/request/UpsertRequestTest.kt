package request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.request.InsertRequestBuilder
import io.supabase.postgrest.query.request.UpsertRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UpsertRequestTest {

    private lateinit var sut: InsertRequestBuilder

    @Test
    fun testUpsertRequestBuilder() {
        sut = UpsertRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            defaultToNull = false
            ignoreDuplicates = true
            count(Count.EXACT)
            select()
        }

        assertEquals("POST", sut.httpMethod.value)
        assertEquals(
            setOf(
                "return=representation",
                "resolution=ignore-duplicates",
                "missing=default",
                "count=exact"
            ), sut.buildPrefer()
        )
    }

    @Test
    fun testUpsertRequestBuilderConflict() {
        sut = UpsertRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            defaultToNull = true
            onConflict = "something"
            count(Count.EXACT)
            select()
        }

        assertEquals("POST", sut.httpMethod.value)
        assertEquals(
            setOf(
                "return=representation",
                "resolution=merge-duplicates",
                "count=exact"
            ), sut.buildPrefer()
        )
        assertNotNull(sut.params["on_conflict"])
        assertEquals("something", sut.params["on_conflict"]!!.first())
    }

    @Test
    fun testUpsertOnConflictProperty() {
        val builder = UpsertRequestBuilder("public", PropertyConversionMethod.NONE)
        assertNull(builder.onConflict)
        builder.onConflict = "id"
        assertEquals("id", builder.onConflict)
        builder.onConflict = null
        assertNull(builder.onConflict)
    }

}