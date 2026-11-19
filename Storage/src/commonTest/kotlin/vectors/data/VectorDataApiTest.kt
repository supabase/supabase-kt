package vectors.data

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.storage.vectors.DistanceMetric
import io.supabase.storage.vectors.data.VectorData
import io.supabase.storage.vectors.data.VectorDataApiImpl
import io.supabase.storage.vectors.data.VectorObject
import io.supabase.testing.MockedHttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class VectorDataApiTest {

    private val bucketName = "test-bucket"
    private val indexName = "test-index"

    @Test
    fun testPutVectors() = runTest {
        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/PutVectors", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        val vectors = listOf(
            VectorObject(
                key = "vec1",
                data = VectorData(floatArrayOf(1.0f, 2.0f, 3.0f)),
                metadata = buildJsonObject { put("source", "test") }
            ),
            VectorObject(
                key = "vec2",
                data = VectorData(floatArrayOf(4.0f, 5.0f, 6.0f)),
                metadata = buildJsonObject { put("source", "test2") }
            )
        )
        vectorDataApi.putVectors(vectors)

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        val vectorsArray = json["vectors"]?.jsonArray
        assertEquals(2, vectorsArray?.size)
        assertEquals("vec1", vectorsArray?.get(0)?.jsonObject?.get("key")?.toString()?.trim('"'))
        assertEquals("vec2", vectorsArray?.get(1)?.jsonObject?.get("key")?.toString()?.trim('"'))
    }

    @Test
    fun testPutVectorsThrowsOnEmptyList() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.putVectors(emptyList())
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("Vector batch size must be between 1 and 500 items", exception?.message)
    }

    @Test
    fun testPutVectorsThrowsOnTooManyVectors() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.putVectors(List(501) { index ->
                VectorObject(
                    key = "vec$index",
                    data = VectorData(floatArrayOf(1.0f)),
                    metadata = buildJsonObject {}
                )
            })
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("Vector batch size must be between 1 and 500 items", exception?.message)
    }

    @Test
    fun testGetVectors() = runTest {
        val responseJson = buildJsonObject {
            put("vectors", buildJsonArray {
                add(buildJsonObject {
                    put("key", "vec1")
                    put("data", buildJsonObject {
                        put("float32", buildJsonArray {
                            add(1.0f)
                            add(2.0f)
                            add(3.0f)
                        })
                    })
                    put("metadata", buildJsonObject {
                        put("source", "test")
                    })
                })
                add(buildJsonObject {
                    put("key", "vec2")
                    put("data", buildJsonObject {
                        put("float32", buildJsonArray {
                            add(4.0f)
                            add(5.0f)
                            add(6.0f)
                        })
                    })
                    put("metadata", buildJsonObject {
                        put("source", "test2")
                    })
                })
            })
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/GetVectors", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        val result = vectorDataApi.getVectors {
            keys.addAll(listOf("vec1", "vec2"))
            returnData = true
            returnMetadata = true
        }

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        assertEquals(2, result.size)
        assertEquals("vec1", result[0].key)
        assertContentEquals(floatArrayOf(1.0f, 2.0f, 3.0f), result[0].data?.float32)
        assertEquals("test", result[0].metadata?.get("source")?.toString()?.trim('"'))
        assertEquals("vec2", result[1].key)
    }

    @Test
    fun testListVectors() = runTest {
        val responseJson = buildJsonObject {
            put("vectors", buildJsonArray {
                add(buildJsonObject {
                    put("key", "vec1")
                    put("data", buildJsonObject {
                        put("float32", buildJsonArray {
                            add(1.0f)
                            add(2.0f)
                        })
                    })
                    put("metadata", buildJsonObject { put("source", "test") })
                })
            })
            put("nextToken", "token123")
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/ListVectors", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        val result = vectorDataApi.listVectors {}

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        assertEquals(1, result.vectors.size)
        assertEquals("vec1", result.vectors[0].key)
        assertEquals("token123", result.nextToken)
    }

    @Test
    fun testListVectorsWithOptions() = runTest {
        val responseJson = buildJsonObject {
            put("vectors", buildJsonArray {})
            put("nextToken", "next-token-123")
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/ListVectors", it.url.toString())
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        val result = vectorDataApi.listVectors {
            maxResults = 100
            nextToken = "prev-token"
            returnData = true
            returnMetadata = false
            segmentCount = 4
            segmentIndex = 2
        }

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))
        assertEquals(100, json["maxResults"]?.toString()?.toInt())
        assertEquals("prev-token", json["nextToken"]?.toString()?.trim('"'))
        assertEquals(true, json["returnData"]?.toString()?.toBoolean())
        assertEquals(false, json["returnMetadata"]?.toString()?.toBoolean())
        assertEquals(4, json["segmentCount"]?.toString()?.toInt())
        assertEquals(2, json["segmentIndex"]?.toString()?.toInt())

        assertEquals("next-token-123", result.nextToken)
    }

    @Test
    fun testListVectorsThrowsOnInvalidSegmentCount() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.listVectors {
                segmentCount = 17
            }
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("segmentCount must be between 1 and 16", exception?.message)
    }

    @Test
    fun testListVectorsThrowsOnInvalidSegmentIndex() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.listVectors {
                segmentCount = 4
                segmentIndex = 4
            }
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("segmentIndex must be between 0 and 3", exception?.message)
    }

    @Test
    fun testQueryVectors() = runTest {
        val responseJson = buildJsonObject {
            put("vectors", buildJsonArray {
                add(buildJsonObject {
                    put("key", "vec1")
                    put("distance", 0.25)
                    put("metadata", buildJsonObject { put("source", "test") })
                })
                add(buildJsonObject {
                    put("key", "vec2")
                    put("distance", 0.5)
                    put("metadata", buildJsonObject { put("source", "test2") })
                })
            })
            put("distanceMetric", "cosine")
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/QueryVectors", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        val result = vectorDataApi.queryVectors {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f, 3.0f))
            topK = 5
            returnDistance = true
            returnMetadata = true
        }

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))
        assertEquals(5, json["topK"]?.toString()?.toInt())
        assertEquals(true, json["returnDistance"]?.toString()?.toBoolean())
        assertEquals(true, json["returnMetadata"]?.toString()?.toBoolean())

        assertEquals(2, result.vectors.size)
        assertEquals("vec1", result.vectors[0].key)
        assertEquals(0.25, result.vectors[0].distance)
        assertEquals("vec2", result.vectors[1].key)
        assertEquals(0.5, result.vectors[1].distance)
        assertEquals(DistanceMetric.COSINE, result.distanceMetric)
    }

    @Test
    fun testQueryVectorsWithFilter() = runTest {
        val responseJson = buildJsonObject {
            put("vectors", buildJsonArray {})
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        vectorDataApi.queryVectors {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
            filter = buildJsonObject {
                put("source", "test")
            }
        }

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        val filterObj = json["filter"]?.jsonObject
        assertEquals("test", filterObj?.get("source")?.toString()?.trim('"'))
    }

    @Test
    fun testDeleteVectors() = runTest {
        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/DeleteVectors", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)
        vectorDataApi.deleteVectors(listOf("vec1", "vec2", "vec3"))

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        val keysArray = json["keys"]?.jsonArray
        assertEquals(3, keysArray?.size)
        assertEquals("vec1", keysArray?.get(0)?.toString()?.trim('"'))
        assertEquals("vec2", keysArray?.get(1)?.toString()?.trim('"'))
        assertEquals("vec3", keysArray?.get(2)?.toString()?.trim('"'))
    }

    @Test
    fun testDeleteVectorsThrowsOnEmptyList() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.deleteVectors(emptyList())
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("Keys batch size must be between 1 and 500 items", exception?.message)
    }

    @Test
    fun testDeleteVectorsThrowsOnTooManyKeys() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorDataApi = VectorDataApiImpl(bucketName, indexName, api)

        var exception: IllegalArgumentException? = null
        try {
            vectorDataApi.deleteVectors(List(501) { "vec$it" })
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertEquals("Keys batch size must be between 1 and 500 items", exception?.message)
    }

}
