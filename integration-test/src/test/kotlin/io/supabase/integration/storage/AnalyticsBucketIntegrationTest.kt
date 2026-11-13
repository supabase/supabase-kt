package io.supabase.integration.storage

import io.supabase.integration.IntegrationTestBase
import io.supabase.storage.BucketSortColumn
import io.supabase.storage.SortOrder
import io.supabase.storage.StorageRestException
import io.supabase.storage.analytics.StorageAnalyticsClient
import io.supabase.storage.storage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsBucketIntegrationTest : IntegrationTestBase() {

    private val analytics: StorageAnalyticsClient by lazy {
        createStatelessClient().storage.analytics
    }

    private val createdBuckets = mutableListOf<String>()

    private fun uniqueName(prefix: String) = "$prefix-${System.nanoTime()}"

    private suspend fun createBucket(name: String = uniqueName("it-analytics")): String {
        // Registered first so the bucket is cleaned up even if decoding the response fails
        createdBuckets += name
        analytics.createBucket(name)
        return name
    }

    @BeforeEach
    fun requireAnalyticsBuckets() = runBlocking {
        val available = try {
            analytics.listBuckets { limit = 1 }
            true
        } catch (e: StorageRestException) {
            if (e.statusCode !in setOf(403, 404, 409)) throw e
            false
        }
        assumeTrue(available, "Analytics buckets are not enabled on $supabaseUrl")
    }

    @AfterEach
    fun deleteBuckets() = runBlocking {
        createdBuckets.forEach { runCatching { analytics.deleteBucket(it) } }
        createdBuckets.clear()
    }

    @Test
    fun testCreateBucket() = runTest {
        val name = uniqueName("it-analytics")

        createdBuckets += name
        val bucket = analytics.createBucket(name)

        assertEquals(name, bucket.name)
    }

    @Test
    fun testCreateDuplicateBucketFails() = runTest {
        val name = createBucket()

        val exception = assertFailsWith<StorageRestException> {
            analytics.createBucket(name)
        }
        assertEquals(409, exception.statusCode, "Unexpected status code: ${exception.message}")
    }

    @Test
    fun testListBucketsWithSearchAndSort() = runTest {
        val prefix = uniqueName("it-analytics-list")
        val names = listOf("$prefix-a", "$prefix-b").onEach { createBucket(it) }

        val ascending = analytics.listBuckets {
            search = prefix
            sortBy(BucketSortColumn.NAME, SortOrder.ASC)
        }
        assertEquals(names, ascending.map { it.name })

        val descending = analytics.listBuckets {
            search = prefix
            sortBy(BucketSortColumn.NAME, SortOrder.DESC)
        }
        assertEquals(names.reversed(), descending.map { it.name })

        val limited = analytics.listBuckets {
            search = prefix
            limit = 1
            offset = 1
            sortBy(BucketSortColumn.NAME, SortOrder.ASC)
        }
        assertEquals(names.drop(1), limited.map { it.name })
    }

    @Test
    fun testDeleteBucket() = runTest {
        val name = createBucket(uniqueName("it-analytics-delete"))

        val message = analytics.deleteBucket(name)

        assertTrue(message.isNotBlank())
        assertFalse(name in analytics.listBuckets { search = name }.map { it.name })
    }

}
