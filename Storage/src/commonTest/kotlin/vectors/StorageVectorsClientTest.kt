package vectors

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.storage.vectors.StorageVectorsClientImpl
import io.supabase.testing.MockedHttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class StorageVectorsClientTest {

    private fun clientResponding(path: String, body: String, onRequest: (String) -> Unit = {}) = StorageVectorsClientImpl(
        AuthenticatedSupabaseApi.minimalAuthenticatedApi(
            httpClient = MockedHttpClient {
                assertEquals("https://supabase.com/$path", it.url.toString())
                assertEquals(HttpMethod.Post, it.method)
                onRequest((it.body as TextContent).text)
                respond(
                    content = body,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        )
    )

    @Test
    fun testGetBucket() = runTest {
        // Response body as returned by the storage server
        var capturedBody: String? = null
        val client = clientResponding(
            "GetVectorBucket",
            """{"vectorBucket":{"vectorBucketName":"embeddings","creationTime":1791210480}}"""
        ) { capturedBody = it }

        val bucket = client.getBucket("embeddings")

        assertEquals("embeddings", Json.parseToJsonElement(capturedBody!!).jsonObject["vectorBucketName"]?.jsonPrimitive?.content)
        assertEquals("embeddings", bucket.vectorBucketName)
        assertEquals(Instant.fromEpochSeconds(1791210480), bucket.creationTime)
        assertNull(bucket.encryptionConfiguration)
    }

    @Test
    fun testGetBucketWithEncryptionConfiguration() = runTest {
        val client = clientResponding(
            "GetVectorBucket",
            """{"vectorBucket":{"vectorBucketName":"embeddings","creationTime":1791210480,"encryptionConfiguration":{"sseType":"AES256"}}}"""
        )

        val bucket = client.getBucket("embeddings")

        assertEquals("AES256", bucket.encryptionConfiguration?.sseType)
        assertNull(bucket.encryptionConfiguration?.kmsKeyArn)
    }

    @Test
    fun testListBuckets() = runTest {
        // Response body as returned by the storage server
        var capturedBody: String? = null
        val client = clientResponding(
            "ListVectorBuckets",
            """{"vectorBuckets":[{"vectorBucketName":"docs-a","creationTime":1791210480},{"vectorBucketName":"docs-b","creationTime":1791210481}],"nextToken":"docs-b"}"""
        ) { capturedBody = it }

        val response = client.listBuckets {
            prefix = "docs-"
            maxResults = 2
        }

        val request = Json.parseToJsonElement(capturedBody!!).jsonObject
        assertEquals("docs-", request["prefix"]?.jsonPrimitive?.content)
        assertEquals("2", request["maxResults"]?.jsonPrimitive?.content)
        assertEquals(listOf("docs-a", "docs-b"), response.vectorBuckets)
        assertEquals("docs-b", response.nextToken)
    }

    @Test
    fun testListBucketsWithoutNextToken() = runTest {
        val client = clientResponding("ListVectorBuckets", """{"vectorBuckets":[]}""")

        val response = client.listBuckets()

        assertEquals(emptyList(), response.vectorBuckets)
        assertNull(response.nextToken)
    }

}
