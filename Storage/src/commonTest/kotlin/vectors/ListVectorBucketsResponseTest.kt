package vectors

import io.supabase.storage.vectors.bucket.ListVectorBucketsResponse
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListVectorBucketsResponseTest {

    @Test
    fun testVectorBucketsProperty() {
        val response = ListVectorBucketsResponse(
            vectorBucketsArray = buildJsonArray {
                add(buildJsonObject { put("vectorBucketName", "bucket-a") })
                add(buildJsonObject { put("vectorBucketName", "bucket-b") })
                add(buildJsonObject { put("vectorBucketName", "bucket-c") })
            },
            nextToken = null
        )

        assertContentEquals(listOf("bucket-a", "bucket-b", "bucket-c"), response.vectorBuckets)
    }

    @Test
    fun testNextTokenProperty() {
        val response = ListVectorBucketsResponse(
            vectorBucketsArray = buildJsonArray {},
            nextToken = "next-page-token-123"
        )

        assertEquals("next-page-token-123", response.nextToken)
    }

    @Test
    fun testWithEmptyBuckets() {
        val response = ListVectorBucketsResponse(
            vectorBucketsArray = buildJsonArray {},
            nextToken = null
        )

        assertEquals(0, response.vectorBuckets.size)
        assertNull(response.nextToken)
    }

    @Test
    fun testWithSingleBucket() {
        val response = ListVectorBucketsResponse(
            vectorBucketsArray = buildJsonArray {
                add(buildJsonObject { put("vectorBucketName", "single-bucket") })
            },
            nextToken = null
        )

        assertEquals(1, response.vectorBuckets.size)
        assertEquals("single-bucket", response.vectorBuckets[0])
    }

    @Test
    fun testWithManyBuckets() {
        val buckets = (1..100).map { "bucket-$it" }
        val response = ListVectorBucketsResponse(
            vectorBucketsArray = buildJsonArray {
                buckets.forEach { name ->
                    add(buildJsonObject { put("vectorBucketName", name) })
                }
            },
            nextToken = "token-for-next-page"
        )

        assertEquals(100, response.vectorBuckets.size)
        assertContentEquals(buckets, response.vectorBuckets)
        assertEquals("token-for-next-page", response.nextToken)
    }

}
