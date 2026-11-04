package io.supabase.integration.storage

import io.supabase.storage.vectors.DistanceMetric
import io.supabase.storage.vectors.index.MetadataConfiguration
import io.supabase.storage.vectors.index.VectorDataType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VectorIndexIntegrationTest : VectorIntegrationTestBase() {

    @Test
    fun testCreateAndGetIndexForEachMetric() = runTest {
        val bucket = createBucket()
        val api = vectors.from(bucket)

        listOf(DistanceMetric.COSINE, DistanceMetric.EUCLIDEAN).forEach { metric ->
            val dimension = if (metric == DistanceMetric.EUCLIDEAN) 1536 else 3
            val name = createIndex(bucket, uniqueName("idx-${metric.value}"), dimension, metric)

            val index = api.getIndex(name)

            assertEquals(name, index.indexName, "metric=$metric")
            assertEquals(bucket, index.vectorBucketName, "metric=$metric")
            assertEquals(VectorDataType.FLOAT32, index.dataType, "metric=$metric")
            assertEquals(dimension, index.dimension, "metric=$metric")
            assertEquals(metric, index.distanceMetric, "metric=$metric")
        }
    }

    @Test
    fun testCreateIndexWithMetadataConfiguration() = runTest {
        val bucket = createBucket()
        val name = uniqueName("idx-meta")
        vectors.from(bucket).createIndex {
            indexName = name
            dataType = VectorDataType.FLOAT32
            dimension = 4
            distanceMetric = DistanceMetric.COSINE
            metadataConfiguration = MetadataConfiguration(nonFilterableMetadataKeys = listOf("raw_text", "source"))
        }

        val index = vectors.from(bucket).getIndex(name)

        assertEquals(listOf("raw_text", "source"), assertNotNull(index.metadataConfiguration).nonFilterableMetadataKeys)
    }

    @Test
    fun testCreateDuplicateIndexFails() = runTest {
        val bucket = createBucket()
        val name = createIndex(bucket)

        assertStorageError(409, "ConflictException") {
            createIndex(bucket, name)
        }
    }

    @Test
    fun testCreateIndexInMissingBucketFails() = runTest {
        assertStorageError(404, "NotFoundException") {
            createIndex(uniqueName("it-vec-missing"))
        }
    }

    @Test
    fun testGetMissingIndexFails() = runTest {
        val bucket = createBucket()

        assertStorageError(404, "NotFoundException") {
            vectors.from(bucket).getIndex(uniqueName("idx-missing"))
        }
    }

    @Test
    fun testListIndexesWithPrefixAndPagination() = runTest {
        val bucket = createBucket()
        val other = createIndex(bucket, "other-index")
        val names = listOf("emb-a", "emb-b", "emb-c").onEach { createIndex(bucket, it) }
        val api = vectors.from(bucket)

        val all = api.listIndexes()
        assertEquals((names + other).sorted(), all.indexes.sorted())

        val firstPage = api.listIndexes {
            prefix = "emb-"
            maxResults = 2
        }
        assertEquals(2, firstPage.indexes.size)
        val token = assertNotNull(firstPage.nextToken)

        val secondPage = api.listIndexes {
            prefix = "emb-"
            maxResults = 2
            nextToken = token
        }
        assertEquals(1, secondPage.indexes.size)
        assertNull(secondPage.nextToken)
        assertEquals(names, (firstPage.indexes + secondPage.indexes).sorted())
    }

    @Test
    fun testDeleteIndex() = runTest {
        val bucket = createBucket()
        val name = createIndex(bucket)
        val api = vectors.from(bucket)

        api.deleteIndex(name)

        assertTrue(api.listIndexes().indexes.isEmpty())
        assertStorageError(404, "NotFoundException") {
            api.getIndex(name)
        }
    }

}
