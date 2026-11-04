package io.supabase.integration.storage

import io.supabase.storage.StorageRestException
import io.supabase.storage.vectors.DistanceMetric
import io.supabase.storage.vectors.data.VectorData
import io.supabase.storage.vectors.data.VectorDataApi
import io.supabase.storage.vectors.data.VectorObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VectorDataIntegrationTest : VectorIntegrationTestBase() {

    private val fixtures = listOf(
        vector("red", floatArrayOf(1f, 0f, 0f), "warm", 1),
        vector("green", floatArrayOf(0f, 1f, 0f), "cool", 2),
        vector("blue", floatArrayOf(0f, 0f, 1f), "cool", 3),
    )

    private fun vector(key: String, data: FloatArray, tone: String, rank: Int) = VectorObject(
        key = key,
        data = VectorData(data),
        metadata = buildJsonObject {
            put("tone", tone)
            put("rank", rank)
        }
    )

    private suspend fun createIndexWithFixtures(metric: DistanceMetric = DistanceMetric.COSINE): VectorDataApi {
        val bucket = createBucket()
        val index = createIndex(bucket, dimension = 3, metric = metric)
        return vectors.from(bucket).index(index).also { it.putVectors(fixtures) }
    }

    @Test
    fun testPutAndGetVectorsWithDataAndMetadata() = runTest {
        val api = createIndexWithFixtures()

        val result = api.getVectors {
            keys += listOf("red", "blue")
            returnData = true
            returnMetadata = true
        }.associateBy { it.key }

        assertEquals(setOf("red", "blue"), result.keys)
        val red = result.getValue("red")
        assertContentEquals(floatArrayOf(1f, 0f, 0f), assertNotNull(red.data).float32)
        val metadata = assertNotNull(red.metadata)
        assertEquals("warm", metadata["tone"]?.jsonPrimitive?.content)
        assertEquals(1, metadata["rank"]?.jsonPrimitive?.int)
    }

    @Test
    fun testGetVectorsOmitsDataAndMetadataByDefault() = runTest {
        val api = createIndexWithFixtures()

        val result = api.getVectors { keys += "green" }

        val green = result.single()
        assertEquals("green", green.key)
        assertNull(green.data)
        assertNull(green.metadata)
    }

    @Test
    fun testGetVectorsSkipsMissingKeys() = runTest {
        val api = createIndexWithFixtures()

        val result = api.getVectors { keys += listOf("red", "does-not-exist") }

        assertEquals(listOf("red"), result.map { it.key })
    }

    @Test
    fun testPutVectorsUpsertsExistingKey() = runTest {
        val api = createIndexWithFixtures()

        api.putVectors(listOf(vector("red", floatArrayOf(0.5f, 0.5f, 0f), "neutral", 9)))

        val red = api.getVectors {
            keys += "red"
            returnData = true
            returnMetadata = true
        }.single()
        assertContentEquals(floatArrayOf(0.5f, 0.5f, 0f), assertNotNull(red.data).float32)
        assertEquals("neutral", assertNotNull(red.metadata)["tone"]?.jsonPrimitive?.content)
    }

    @Test
    fun testPutMaximumBatch() = runTest {
        val bucket = createBucket()
        val api = vectors.from(bucket).index(createIndex(bucket, dimension = 2))
        val batch = (1..VectorDataApi.VECTOR_RANGE.last).map {
            VectorObject("key-$it", VectorData(floatArrayOf(it.toFloat(), 1f)), JsonObject(emptyMap()))
        }

        api.putVectors(batch)

        val keys = mutableListOf<String>()
        var token: String? = null
        do {
            val page = api.listVectors {
                maxResults = 200
                nextToken = token
            }
            keys += page.vectors.map { it.key }
            token = page.nextToken
        } while (token != null)
        assertEquals(batch.map { it.key }.toSet(), keys.toSet())
    }

    @Test
    fun testPutVectorsWithWrongDimensionFails() = runTest {
        val api = createIndexWithFixtures()

        // Newer storage servers answer 400 InvalidParameter; older pgvector images answer 500.
        assertFailsWith<StorageRestException> {
            api.putVectors(listOf(vector("bad", floatArrayOf(1f, 2f), "warm", 0)))
        }
    }

    @Test
    fun testListVectorsPagination() = runTest {
        val api = createIndexWithFixtures()

        val firstPage = api.listVectors { maxResults = 2 }
        assertEquals(2, firstPage.vectors.size)
        val token = assertNotNull(firstPage.nextToken)

        val secondPage = api.listVectors {
            maxResults = 2
            nextToken = token
        }
        assertEquals(1, secondPage.vectors.size)
        assertNull(secondPage.nextToken)

        val keys = (firstPage.vectors + secondPage.vectors).map { it.key }
        assertEquals(fixtures.map { it.key }.toSet(), keys.toSet())
        assertTrue((firstPage.vectors + secondPage.vectors).all { it.data == null && it.metadata == null })
    }

    @Test
    fun testListVectorsWithDataAndMetadata() = runTest {
        val api = createIndexWithFixtures()

        val result = api.listVectors {
            returnData = true
            returnMetadata = true
        }

        assertEquals(fixtures.size, result.vectors.size)
        result.vectors.forEach {
            assertNotNull(it.data)
            assertNotNull(it.metadata)
        }
    }

    @Test
    fun testListVectorsSegmentsCoverAllKeys() = runTest {
        val api = createIndexWithFixtures()

        val keys = (0 until 2).flatMap { segment ->
            api.listVectors {
                segmentCount = 2
                segmentIndex = segment
            }.vectors.map { it.key }
        }

        assertEquals(fixtures.map { it.key }.sorted(), keys.sorted())
    }

    @Test
    fun testQueryVectorsCosine() = runTest {
        val api = createIndexWithFixtures(DistanceMetric.COSINE)

        val result = api.queryVectors {
            queryVector = VectorData(floatArrayOf(1f, 0.1f, 0f))
            topK = 2
            returnDistance = true
            returnMetadata = true
        }

        assertEquals(DistanceMetric.COSINE, result.distanceMetric)
        assertEquals(listOf("red", "green"), result.vectors.map { it.key })
        val distances = result.vectors.map { assertNotNull(it.distance).toDouble() }
        assertTrue(distances[0] in 0.0..0.01, "Expected red to be almost identical, got ${distances[0]}")
        assertTrue(distances[0] < distances[1], "Results must be ordered by distance: $distances")
        assertEquals("warm", assertNotNull(result.vectors[0].metadata)["tone"]?.jsonPrimitive?.content)
    }

    @Test
    fun testQueryVectorsEuclidean() = runTest {
        val api = createIndexWithFixtures(DistanceMetric.EUCLIDEAN)

        val result = api.queryVectors {
            queryVector = VectorData(floatArrayOf(0f, 0f, 2f))
            topK = 1
            returnDistance = true
        }

        assertEquals(DistanceMetric.EUCLIDEAN, result.distanceMetric)
        val match = result.vectors.single()
        assertEquals("blue", match.key)
        assertEquals(1.0, assertNotNull(match.distance).toDouble(), 0.01)
    }

    @Test
    fun testQueryVectorsOmitsDistanceAndMetadataByDefault() = runTest {
        val api = createIndexWithFixtures()

        val result = api.queryVectors {
            queryVector = VectorData(floatArrayOf(1f, 0f, 0f))
            topK = 3
        }

        assertEquals(3, result.vectors.size)
        assertTrue(result.vectors.all { it.distance == null && it.metadata == null && it.data == null })
    }

    @Test
    fun testQueryVectorsWithMetadataFilter() = runTest {
        val api = createIndexWithFixtures()

        val equality = api.queryVectors {
            queryVector = VectorData(floatArrayOf(1f, 0f, 0f))
            topK = 10
            filter = buildJsonObject { put("tone", "cool") }
        }
        assertEquals(setOf("green", "blue"), equality.vectors.map { it.key }.toSet())

        val range = api.queryVectors {
            queryVector = VectorData(floatArrayOf(1f, 0f, 0f))
            topK = 10
            filter = buildJsonObject {
                putJsonObject("rank") { put("\$gte", 3) }
            }
        }
        assertEquals(setOf("blue"), range.vectors.map { it.key }.toSet())
    }

    @Test
    fun testDeleteVectors() = runTest {
        val api = createIndexWithFixtures()

        api.deleteVectors(listOf("red", "does-not-exist"))

        val remaining = api.listVectors().vectors.map { it.key }.toSet()
        assertEquals(setOf("green", "blue"), remaining)
        assertTrue(api.getVectors { keys += "red" }.isEmpty())
    }

    @Test
    fun testOperationsOnMissingIndexFail() = runTest {
        val bucket = createBucket()
        val api = vectors.from(bucket).index(uniqueName("idx-missing"))

        assertStorageError(404, "NotFoundException") {
            api.putVectors(fixtures)
        }
        assertStorageError(404, "NotFoundException") {
            api.queryVectors {
                queryVector = VectorData(floatArrayOf(1f, 0f, 0f))
                topK = 1
            }
        }
    }

}
