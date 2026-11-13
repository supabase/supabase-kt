package analytics

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.storage.Storage
import io.supabase.storage.storage
import io.supabase.storage.analytics.StorageAnalyticsClientImpl
import io.supabase.testing.MockedHttpClient
import io.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageAnalyticsClientTest {

    private val mockUrl = "https://supabase.com"

    @Test
    fun testCreateBucket() = runTest {
        val responseJson = buildJsonObject {
            put("name", "my-analytics-bucket")
            put("type", "ANALYTICS")
            put("format", "iceberg")
            put("created_at", "2024-01-01T00:00:00Z")
            put("updated_at", "2024-01-01T00:00:00Z")
        }

        var capturedBody: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("$mockUrl/bucket", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                capturedBody = (it.body as TextContent).text
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val client = StorageAnalyticsClientImpl(api)
        val result = client.createBucket("my-analytics-bucket")

        val json = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals("my-analytics-bucket", json["name"]?.toString()?.trim('"'))

        assertEquals("my-analytics-bucket", result.name)
        assertEquals("ANALYTICS", result.type)
        assertEquals("iceberg", result.format)
    }

    @Test
    fun testCreateBucketWithServerResponseShape() = runTest {
        // The storage server only returns id, name and timestamps for analytics buckets
        val responseJson = buildJsonObject {
            put("id", "my-analytics-bucket")
            put("name", "my-analytics-bucket")
            put("created_at", "2024-01-01T00:00:00Z")
            put("updated_at", "2024-01-01T00:00:00Z")
        }
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val result = StorageAnalyticsClientImpl(api).createBucket("my-analytics-bucket")

        assertEquals("my-analytics-bucket", result.name)
        assertEquals("ANALYTICS", result.type)
        assertEquals("iceberg", result.format)
    }

    @Test
    fun testListBuckets() = runTest {
        val responseJson = buildJsonArray {
            add(buildJsonObject {
                put("name", "bucket1")
                put("type", "ANALYTICS")
                put("format", "iceberg")
                put("created_at", "2024-01-01T00:00:00Z")
                put("updated_at", "2024-01-01T00:00:00Z")
            })
            add(buildJsonObject {
                put("name", "bucket2")
                put("type", "ANALYTICS")
                put("format", "iceberg")
                put("created_at", "2024-01-02T00:00:00Z")
                put("updated_at", "2024-01-02T00:00:00Z")
            })
        }

        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("$mockUrl/bucket", it.url.toString())
                assertEquals(HttpMethod.Get, it.method)
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val client = StorageAnalyticsClientImpl(api)
        val result = client.listBuckets {}

        assertEquals(2, result.size)
        assertEquals("bucket1", result[0].name)
        assertEquals("bucket2", result[1].name)
    }

    @Test
    fun testListBucketsWithFilter() = runTest {
        val responseJson = buildJsonArray {}

        var capturedUrl: String? = null
        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                capturedUrl = it.url.toString()
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val client = StorageAnalyticsClientImpl(api)
        client.listBuckets {
            limit = 50
            offset = 10
            search = "my-bucket"
        }

        assertEquals(true, capturedUrl?.contains("limit=50"))
        assertEquals(true, capturedUrl?.contains("offset=10"))
        assertEquals(true, capturedUrl?.contains("search=my-bucket"))
    }

    @Test
    fun testDeleteBucket() = runTest {
        val responseJson = buildJsonObject {
            put("message", "Bucket deleted successfully")
        }

        val api = AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("$mockUrl/bucket/bucket-to-delete", it.url.toString())
                assertEquals(HttpMethod.Delete, it.method)
                respond(
                    content = responseJson.toString(),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
        val client = StorageAnalyticsClientImpl(api)
        val result = client.deleteBucket("bucket-to-delete")

        assertEquals("Bucket deleted successfully", result)
    }

    @Test
    fun testIcebergClientConfiguration() = runTest {
        val client = createMockedSupabaseClient(
            supabaseUrl = "https://projectref.supabase.co",
            supabaseKey = "project-anon-key",
            configuration = { install(Storage) }
        ) { error("getIcebergClientConfiguration must not make a request") }

        val config = client.storage.analytics.getIcebergClientConfiguration()

        assertTrue(config.baseUrl.trimEnd('/').endsWith("/storage/v1/iceberg"), config.baseUrl)
        assertEquals("project-anon-key", config.defaultHeaders()["apikey"])
    }

}
