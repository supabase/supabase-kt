package request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.request.UpdateRequestBuilder
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateRequestTest {

    private lateinit var sut: UpdateRequestBuilder

    @Test
    fun testUpdate() {
        sut = UpdateRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            count(Count.EXACT)
            select()
        }

        assertEquals(HttpMethod.Patch, sut.httpMethod)
        assertEquals(
            setOf(
                "return=representation",
                "count=exact"
            ), sut.buildPrefer()
        )
        assertEquals("public", sut.schema)
    }


}