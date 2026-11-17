package vectors.index

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.storage.vectors.DistanceMetric
import io.supabase.storage.vectors.data.VectorDataApi
import io.supabase.storage.vectors.index.MetadataConfiguration
import io.supabase.storage.vectors.index.VectorDataType
import io.supabase.storage.vectors.index.VectorIndexApiImpl
import io.supabase.testing.MockedHttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VectorIndexApiTest {

    private val bucketName = "test-bucket"
    private val indexName = "test-index"

    @Test
    fun testCreateIndex() = runTest {
        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/CreateIndex", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond("")
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        vectorIndex.createIndex {
            this.indexName = "my-index"
            this.dataType = VectorDataType.FLOAT32
            this.dimension = 384
            this.distanceMetric = DistanceMetric.COSINE
        }

        // Verify the request body contains all the options
        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals("my-index", json["indexName"]?.toString()?.trim('"'))
        assertEquals("float32", json["dataType"]?.toString()?.trim('"'))
        assertEquals(384, json["dimension"]?.toString()?.toInt())
        assertEquals("cosine", json["distanceMetric"]?.toString()?.trim('"'))
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
    }

    @Test
    fun testCreateIndexWithMetadataConfiguration() = runTest {
        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/CreateIndex", it.url.toString())
                capturedBody = (it.body as TextContent).text
                respond("")
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        vectorIndex.createIndex {
            this.indexName = "my-index"
            this.dataType = VectorDataType.FLOAT32
            this.dimension = 768
            this.distanceMetric = DistanceMetric.EUCLIDEAN
            this.metadataConfiguration = MetadataConfiguration(
                nonFilterableMetadataKeys = listOf("description", "raw_text")
            )
        }

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals("my-index", json["indexName"]?.toString()?.trim('"'))
        assertEquals("euclidean", json["distanceMetric"]?.toString()?.trim('"'))
        assertEquals(768, json["dimension"]?.toString()?.toInt())

        val metadataConfig = json["metadataConfiguration"]?.jsonObject
        val nonFilterableKeys = metadataConfig?.get("nonFilterableMetadataKeys")?.jsonArray
        assertEquals(2, nonFilterableKeys?.size)
        assertEquals("description", nonFilterableKeys?.get(0)?.toString()?.trim('"'))
        assertEquals("raw_text", nonFilterableKeys?.get(1)?.toString()?.trim('"'))
    }

    @Test
    fun testListIndexes() = runTest {
        val responseJson = buildJsonObject {
            put("indexes", buildJsonArray {
                add(buildJsonObject {
                    put("indexName", "index1")
                })
                add(buildJsonObject {
                    put("indexName", "index2")
                })
                add(buildJsonObject {
                    put("indexName", "index3")
                })
            })
            put("nextToken", "token123")
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/ListIndexes", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        val result = vectorIndex.listIndexes {}

        // Verify the request body contains the bucket name
        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))

        // Verify the response
        assertContentEquals(listOf("index1", "index2", "index3"), result.indexes)
        assertEquals("token123", result.nextToken)
    }

    @Test
    fun testListIndexesWithOptions() = runTest {
        val responseJson = buildJsonObject {
            put("indexes", buildJsonArray {
                add(buildJsonObject {
                    put("indexName", "my-prefix-index1")
                })
            })
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/ListIndexes", it.url.toString())
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        val result = vectorIndex.listIndexes {
            prefix = "my-prefix"
            maxResults = 10
            nextToken = "previous-token"
        }

        // Verify the request body contains all options
        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals("my-prefix", json["prefix"]?.toString()?.trim('"'))
        assertEquals(10, json["maxResults"]?.toString()?.toInt())
        assertEquals("previous-token", json["nextToken"]?.toString()?.trim('"'))

        assertContentEquals(listOf("my-prefix-index1"), result.indexes)
    }

    @Test
    fun testGetIndex() = runTest {
        val indexName = "my-index"
        val responseJson = buildJsonObject {
            put("index", buildJsonObject {
                put("indexName", indexName)
                put("vectorBucketName", bucketName)
                put("dataType", "float32")
                put("dimension", 384)
                put("distanceMetric", "cosine")
                put("creationTime", 1234567890)
            })
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/GetIndex", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        val result = vectorIndex.getIndex(indexName)

        // Verify the request body
        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        // Verify the response
        assertEquals(indexName, result.indexName)
        assertEquals(bucketName, result.vectorBucketName)
        assertEquals(VectorDataType.FLOAT32, result.dataType)
        assertEquals(384, result.dimension)
        assertEquals(DistanceMetric.COSINE, result.distanceMetric)
    }

    @Test
    fun testDeleteIndex() = runTest {
        val indexName = "index-to-delete"
        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/DeleteIndex", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond("")
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        vectorIndex.deleteIndex(indexName)

        // Verify the request body
        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))
    }

    @Test
    fun testIndexMethodReturnsVectorDataApi() = runTest {
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond("")
            }
        )
        val vectorIndex = VectorIndexApiImpl(bucketName, api)
        val vectorDataApi = vectorIndex.index("my-data-index")

        assertIs<VectorDataApi>(vectorDataApi)
    }

}
