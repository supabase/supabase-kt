package io.supabase.integration.storage

import io.supabase.storage.storage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VectorBucketIntegrationTest : VectorIntegrationTestBase() {

    @Test
    fun testCreateAndGetBucket() = runTest {
        val name = createBucket()

        val bucket = vectors.getBucket(name)

        assertEquals(name, bucket.vectorBucketName)
        assertNotNull(bucket.creationTime)
    }

    @Test
    fun testCreateDuplicateBucketFails() = runTest {
        val name = createBucket()

        assertStorageError(409, "ConflictException") {
            vectors.createBucket(name)
        }
    }

    @Test
    fun testGetMissingBucketFails() = runTest {
        assertStorageError(404, "NotFoundException") {
            vectors.getBucket(uniqueName("it-vec-missing"))
        }
    }

    @Test
    fun testListBucketsWithUnknownPrefixIsEmpty() = runTest {
        val response = vectors.listBuckets { prefix = uniqueName("it-vec-none") }

        assertTrue(response.vectorBuckets.isEmpty())
        assertNull(response.nextToken)
    }

    @Test
    fun testListBucketsPagination() = runTest {
        val prefix = uniqueName("it-vec-page")
        val names = listOf("$prefix-a", "$prefix-b", "$prefix-c").onEach { createBucket(it) }

        val firstPage = vectors.listBuckets {
            this.prefix = prefix
            maxResults = 2
        }
        assertEquals(2, firstPage.vectorBuckets.size)
        val token = assertNotNull(firstPage.nextToken)

        val secondPage = vectors.listBuckets {
            this.prefix = prefix
            maxResults = 2
            nextToken = token
        }
        assertEquals(1, secondPage.vectorBuckets.size)
        assertNull(secondPage.nextToken)

        assertEquals(names, (firstPage.vectorBuckets + secondPage.vectorBuckets).sorted())
    }

    @Test
    fun testDeleteBucket() = runTest {
        val name = createBucket(uniqueName("it-vec-delete"))

        vectors.deleteBucket(name)

        assertStorageError(404, "NotFoundException") {
            vectors.getBucket(name)
        }
    }

    @Test
    fun testDeleteBucketWithIndexFails() = runTest {
        val name = createBucket()
        val index = createIndex(name)

        assertStorageError(400, "VectorBucketNotEmpty") {
            vectors.deleteBucket(name)
        }

        // Once the index is gone the bucket can be deleted; testDeleteBucket covers the 404 afterwards
        vectors.from(name).deleteIndex(index)
        vectors.deleteBucket(name)
    }

    @Test
    fun testAnonKeyIsDenied() = runTest {
        val anonVectors = createStatelessClient(key = supabaseAnonKey).storage.vectors

        assertStorageError(403, "AccessDenied") {
            anonVectors.listBuckets()
        }
    }

}
