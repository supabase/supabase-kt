import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.RpcMethod
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PostgrestTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Postgrest)
    }
    
    private lateinit var supabase: SupabaseClient

    @Test
    fun testMultipleFiltersOnOneColumn() = runTest {
        var sent: List<String>? = null
        val supabase = createSupabaseClient("https://example.supabase.co", "anon-key") {
            httpEngine = MockEngine { request ->
                sent = request.url.parameters.getAll("age")
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            install(Postgrest)
        }

        supabase.from("people").select {
            filter {
                gte("age", 1)
                lte("age", 10)
            }
        }

        assertEquals(listOf("gte.1", "lte.10"), sent) // fails: sent is ["gte.1"]
    }
    @Test
    fun testUrlLengthLimit() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond("")
        }
        runTest {
            assertFailsWith<IllegalStateException> {
                supabase.from("schema", "table").select {
                    filter {
                        eq("someKey", "a".repeat(8000)) //just to hit the limit
                    }
                }
            }
        }
    }

    @Test
    fun testSelect() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from(table).select(columns)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testSingleThrowsOnErrorResponse() {
        assertFailsWith<PostgrestRestException> {
            testClient(
                request = { table ->
                    from(table).select() {
                        single()
                    }
                },
                requestHandler = {
                    respond("""
                    { "code": "PGRST116", "details": "The result contains 0 rows", "hint": null, "message": "JSON object requested, multiple (or no) rows returned"}
                """.trimIndent(), HttpStatusCode.BadRequest)
                }
            )
        }
    }

    @Test
    fun testMaybeSingleDoesntThrowOnErrorResponse() {
        testClient(
            request = { table ->
                val result = from(table).select() {
                    maybeSingle()
                }
                assertEquals("", result.data)
                result
            },
            requestHandler = {
                respond("""
                    { "code": "PGRST116", "details": "The result contains 0 rows", "hint": null, "message": "JSON object requested, multiple (or no) rows returned"}
                """.trimIndent(), HttpStatusCode.BadRequest)
            }
        )
    }

    @Test
    fun testSelectSchema() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from("schema", table).select(columns)
            },
            requestHandler = {
                assertEquals("schema", it.headers["Accept-Profile"])
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testOperatorGet() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                postgrest[table].select(columns)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testOperatorGetSchema() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                postgrest["schema", table].select(columns)
            },
            requestHandler = {
                assertEquals("schema", it.headers["Accept-Profile"])
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testSelectHttpMethodHead() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from(table).select(columns) {
                    head = true
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Head, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testSelectHttpMethodHeadColumnList() {
        val columns = Columns.list(listOf("column1", "column2"))
        testClient(
            request = { table ->
                from(table).select(columns) {
                    head = true
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Head, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testInsertValues() {
        val values = listOf(TestObject("mockData1"), TestObject("mockData2"))
        val expectedBody = JsonArray(listOf(
            buildJsonObject { put("data", "mockData1") },
            buildJsonObject { put("data", "mockData2") }
        ))
        testClient(
            request = { table ->
                from("schema", table).insert(values)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Post, it.method)
                val receivedBody = it.body.toJsonElement()
                assertEquals(expectedBody, receivedBody)
                assertEquals("\"data\"", it.url.parameters["columns"])
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpsertValues() {
        val values = listOf(TestObject("mockData1"), TestObject("mockData2"))
        val expectedBody = JsonArray(listOf(
            buildJsonObject { put("data", "mockData1") },
            buildJsonObject { put("data", "mockData2") }
        ))
        testClient(
            request = { table ->
                from("schema", table).upsert(values)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Post, it.method)
                val receivedBody = it.body.toJsonElement()
                assertEquals(expectedBody, receivedBody)
                assertEquals("\"data\"", it.url.parameters["columns"])
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testInsertQuotesColumnNames() {
        val body = JsonArray(listOf(buildJsonObject {
            put("price (usd)", 1)
            put("a.b", 2)
        }))
        testClient(
            request = { table ->
                from(table).insert(body)
            },
            requestHandler = {
                assertEquals("\"price (usd)\",\"a.b\"", it.url.parameters["columns"])
                respond("")
            }
        )
    }

    @Test
    fun testUpsertQuotesColumnNames() {
        val body = JsonArray(listOf(buildJsonObject {
            put("price (usd)", 1)
            put("a.b", 2)
        }))
        testClient(
            request = { table ->
                from(table).upsert(body)
            },
            requestHandler = {
                assertEquals("\"price (usd)\",\"a.b\"", it.url.parameters["columns"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdate() {
        val body = buildJsonObject {
            put("key", "value")
            put("key2", "value")
        }
        testClient(
            request = { table ->
                from("schema", table).update(body) {
                    assertEquals(body, this.body)
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                val receivedBody = it.body.toJsonElement()
                assertEquals(body, receivedBody)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdateManual() {
        testClient(
            request = { table ->
                from("schema", table).update({
                    set("key", "value")
                    set("key2", "value2")
                }) {
                    assertEquals(body, this.body)
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                val receivedBody = it.body.toJsonElement().jsonObject
                assertEquals("value", receivedBody["key"]?.jsonPrimitive?.content)
                assertEquals("value2", receivedBody["key2"]?.jsonPrimitive?.content)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdateValue() {
        val value = TestObject.mock
        val expectedBody = buildJsonObject { put("data", "mockData") }
        testClient(
            request = { table ->
                from("schema", table).update(value)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                val receivedBody = it.body.toJsonElement()
                assertEquals(expectedBody, receivedBody)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testDelete() {
        testClient(
            request = { table ->
                from("schema", table).delete {
                    filter {
                        eq("key", "value")
                    }
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                assertEquals("eq.value", it.url.parameters["key"])
                respond("")
            }
        )
    }

    @Test
    fun testDeleteWithEmptyLikeAllPattern() {
        testClient(
            request = { table ->
                from("schema", table).delete {
                    filter {
                        likeAll("code", listOf(""))
                    }
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertEquals("like(all).{\"\"}", it.url.parameters["code"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdateWithRequest() {
        testClient(
            request = { table ->
                from("schema", table).update({
                    set("key", "value")
                }) {
                    select()
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                val preferSet = it.headers["Prefer"]?.split(",")?.toSet() ?: emptySet()
                assertEquals(setOf("return=representation"), preferSet)
                assertEquals("*", it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testDeleteWithRequest() {
        testClient(
            request = { table ->
                from("schema", table).delete {
                    filter {
                        eq("key", "value")
                    }
                    count(Count.ESTIMATED)
                    select()
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                assertEquals("eq.value", it.url.parameters["key"])
                val preferSet = it.headers["Prefer"]?.split(",")?.toSet() ?: emptySet()
                assertEquals(setOf("count=estimated", "return=representation"), preferSet)
                assertEquals("*", it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testUpsertWithRequest() {
        val body = JsonArray(listOf(buildJsonObject { put("key", "value") }))
        testClient(
            request = { table ->
                from("schema", table).upsert(body) {
                    onConflict = "key"
                    ignoreDuplicates = true
                    select()
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Post, it.method)
                assertEquals("key", it.url.parameters["on_conflict"])
                val preferSet = it.headers["Prefer"]?.split(",")?.toSet() ?: emptySet()
                assertEquals(setOf("resolution=ignore-duplicates", "return=representation"), preferSet)
                assertEquals("*", it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testDeleteEmptyParams() {
        testClient(
            request = { table ->
                from("schema", table).delete()
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdateEmptyParams() {
        testClient(
            request = { table ->
                from("schema", table).update({ set("key", "value") })
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpsertEmptyParams() {
        val body = JsonArray(listOf(buildJsonObject { put("key", "value") }))
        testClient(
            request = { table ->
                from("schema", table).upsert(body)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Post, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testInsertEmptyParams() {
        val body = JsonArray(listOf(buildJsonObject { put("key", "value") }))
        testClient(
            request = { table ->
                from("schema", table).insert(body)
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Post, it.method)
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testRpcNoParameters() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Head, it.method)
            assertEquals("schema", it.headers["Accept-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function") {
                method = RpcMethod.HEAD
                schema = "schema"
            }
        }
    }

    @Test
    fun testRpcGenericParameters() {
        val mockData = TestObject.mock
        val expectedJson = buildJsonObject { put("data", mockData.data) }
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertEquals(expectedJson, it.body.toJsonElement())
            assertMethodIs(HttpMethod.Post, it.method)
            assertEquals("schema", it.headers["Content-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function", mockData) {
                schema = "schema"
            }
        }
    }

    @Test
    fun testRpcGenericParametersDefaultRequest() {
        val mockData = TestObject.mock
        val expectedJson = buildJsonObject { put("data", mockData.data) }
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertEquals(expectedJson, it.body.toJsonElement())
            assertMethodIs(HttpMethod.Post, it.method)
            assertEquals("public", it.headers["Content-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function", mockData)
        }
    }

    @Test
    fun testRpcReturnsNull() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond("null")
        }

        runTest {
            val result = supabase.postgrest.rpc("get_nullable_value")
            val value = result.decodeAs<String?>()
            assertNull(value)
        }
    }

    @Test
    fun testRpcReturnsStringNull() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond("\"null\"")
        }

        runTest {
            val result = supabase.postgrest.rpc("get_text_value")
            val value = result.decodeAs<String?>()
            assertEquals("null", value)
        }
    }

    @Test
    fun testRpcNoParametersDefaultRequest() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            assertEquals("public", it.headers["Content-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function")
        }
    }

    @Test
    fun testRpcParametersDefaultRequest() {
        val mockData = buildJsonObject {
            put("key", "value")
        }
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertEquals(mockData, it.body.toJsonElement())
            assertMethodIs(HttpMethod.Post, it.method)
            assertEquals("public", it.headers["Content-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function", mockData)
        }
    }

    @Test
    fun testRpcParametersWithHeadMethod() {
        val mockData = buildJsonObject {
            put("key", "value")
        }
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Head, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals("value", body["key"]?.jsonPrimitive?.content)
            assertEquals("schema", it.headers["Accept-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function", mockData) {
                schema = "schema"
                method = RpcMethod.HEAD
            }
        }
    }

    /** Decode Truth Table **/

    @Serializable
    data class TestObject(val data: String) {
        companion object {
            val mock = TestObject("mockData")
        }
    }

    @Test
    fun testDecodeAsNullableTypeMatchingResponseAndResult() {
        supabase = createMockedSupabaseClient(configuration = configureClient) {
            respond(Json.encodeToString(TestObject.mock))
        }
        runTest {
            val result = supabase.postgrest.rpc("function")
            val value = result.decodeAs<TestObject?>()
            assertEquals(TestObject.mock, value)
        }
    }

    @Test
    fun testDecodeAsNullableTypeMismatchedResponseFailure() {
        supabase = createMockedSupabaseClient(configuration = configureClient) {
            respond("[]")
        }
        runTest {
            val result = supabase.postgrest.rpc("function")
            assertFailsWith<SerializationException> {
                result.decodeAs<TestObject?>()
            }
        }
    }

    @Test
    fun testDecodeAsNullableTypeNullResponseNullResult() {
        supabase = createMockedSupabaseClient(configuration = configureClient) {
            respond("null")
        }
        runTest {
            val result = supabase.postgrest.rpc("function")
            val value = result.decodeAs<TestObject?>()
            assertNull(value)
        }
    }

    private fun testClient(
        table: String = "table",
        request: suspend SupabaseClient.(table: String) -> PostgrestResult,
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("")},
    ) {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/$table", it.url.pathAfterVersion())
            requestHandler(it)
        }
        runTest {
            supabase.request(table)
        }
    }

    @Test
    fun testParseErrorResponse() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond(
                content = """{"message": "error msg", "hint": "error hint", "details": "error details", "code": "123"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        runTest {
            val exception = assertFailsWith<PostgrestRestException> {
                supabase.postgrest["table"].select()
            }
            assertEquals("error msg", exception.error)
            assertEquals("error hint", exception.hint)
            assertEquals("error details", exception.details?.jsonPrimitive?.content)
            assertEquals("123", exception.code)
        }
    }

    @Test
    fun testParseErrorResponseMessage() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond(
                content = """{"message": "error msg", "hint": "error hint", "details": "error details", "code": "123"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        runTest {
            val exception = assertFailsWith<PostgrestRestException> {
                supabase.postgrest["table"].select()
            }
            assertEquals(HttpStatusCode.BadRequest.value, exception.statusCode)
            assertEquals(
                """
                    Code: 123
                    Hint: error hint
                    Details: "error details"
                """.trimIndent(),
                exception.description
            )
        }
    }

    @Test
    fun testParseErrorResponseNullBody() {
        supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }
        runTest {
            val exception = assertFailsWith<PostgrestRestException> {
                supabase.postgrest["table"].select()
            }
            assertEquals("Unknown error", exception.error)
            assertNull(exception.hint)
            assertNull(exception.details)
            assertNull(exception.code)
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::supabase.isInitialized) {
                supabase.close()
            }
        }
    }

}