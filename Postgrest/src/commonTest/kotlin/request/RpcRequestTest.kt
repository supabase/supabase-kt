package request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.RpcMethod
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.request.RpcRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class RpcRequestTest {

    private lateinit var sut: RpcRequestBuilder

    @Test
    fun testRpcWithoutCustomMethod() {
        sut = RpcRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            count(Count.EXACT)
        }

        assertEquals("POST", sut.httpMethod.value)
        assertEquals(
            setOf(
                "count=exact"
            ), sut.buildPrefer()
        )
        assertEquals("public", sut.schema)
    }

    @Test
    fun testRpc() {
        sut = RpcRequestBuilder("public", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE).apply {
            count(Count.ESTIMATED)
            method = RpcMethod.GET
        }

        assertEquals("GET", sut.httpMethod.value)
        assertEquals(
            setOf(
                "count=estimated"
            ), sut.buildPrefer()
        )
        assertEquals("public", sut.schema)
    }

}