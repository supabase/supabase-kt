package bucket

import io.supabase.storage.vectors.bucket.VectorBucketFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VectorBucketFilterTest {

    @Test
    fun testBuildWithPrefix() {
        val filter = VectorBucketFilter().apply {
            prefix = "my-prefix"
        }

        val json = filter.build()
        assertEquals("my-prefix", json["prefix"]?.toString()?.trim('"'))
    }

    @Test
    fun testBuildWithMaxResults() {
        val filter = VectorBucketFilter().apply {
            maxResults = 50
        }

        val json = filter.build()
        assertEquals(50, json["maxResults"]?.toString()?.toInt())
    }

    @Test
    fun testBuildWithNextToken() {
        val filter = VectorBucketFilter().apply {
            nextToken = "pagination-token-123"
        }

        val json = filter.build()
        assertEquals("pagination-token-123", json["nextToken"]?.toString()?.trim('"'))
    }

    @Test
    fun testBuildWithAllOptions() {
        val filter = VectorBucketFilter().apply {
            prefix = "test-bucket-"
            maxResults = 100
            nextToken = "next-page-token"
        }

        val json = filter.build()
        assertEquals("test-bucket-", json["prefix"]?.toString()?.trim('"'))
        assertEquals(100, json["maxResults"]?.toString()?.toInt())
        assertEquals("next-page-token", json["nextToken"]?.toString()?.trim('"'))
    }

    @Test
    fun testBuildWithoutOptionalFields() {
        val filter = VectorBucketFilter()

        val json = filter.build()
        assertNull(json["prefix"])
        assertNull(json["maxResults"])
        assertNull(json["nextToken"])
    }

    @Test
    fun testBuildWithOnlyPrefix() {
        val filter = VectorBucketFilter().apply {
            prefix = "prod-"
        }

        val json = filter.build()
        assertEquals("prod-", json["prefix"]?.toString()?.trim('"'))
        assertNull(json["maxResults"])
        assertNull(json["nextToken"])
    }

}
