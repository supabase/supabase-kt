package io.supabase.integration.storage

import io.supabase.integration.IntegrationTestBase
import io.supabase.storage.StorageRestException
import io.supabase.storage.storage
import io.supabase.storage.vectors.DistanceMetric
import io.supabase.storage.vectors.StorageVectorsClient
import io.supabase.storage.vectors.index.VectorDataType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

abstract class VectorIntegrationTestBase : IntegrationTestBase() {

    protected val vectors: StorageVectorsClient by lazy {
        createStatelessClient().storage.vectors
    }

    private val createdBuckets = mutableListOf<String>()

    protected fun uniqueName(prefix: String) = "$prefix-${System.nanoTime()}"

    protected suspend fun createBucket(name: String = uniqueName("it-vec")): String {
        createdBuckets += name
        vectors.createBucket(name)
        return name
    }

    protected suspend fun createIndex(
        bucket: String,
        name: String = uniqueName("idx"),
        dimension: Int = 3,
        metric: DistanceMetric = DistanceMetric.COSINE,
    ): String {
        vectors.from(bucket).createIndex {
            indexName = name
            dataType = VectorDataType.FLOAT32
            this.dimension = dimension
            distanceMetric = metric
        }
        return name
    }

    protected suspend fun assertStorageError(
        statusCode: Int,
        code: String,
        block: suspend () -> Unit
    ): StorageRestException {
        val exception = assertFailsWith<StorageRestException> { block() }
        assertEquals(statusCode, exception.statusCode, "Unexpected status code: ${exception.message}")
        assertEquals(code, exception.code, "Unexpected error code: ${exception.message}")
        return exception
    }

    @AfterEach
    fun deleteBuckets() = runBlocking {
        createdBuckets.forEach { bucket ->
            runCatching {
                val api = vectors.from(bucket)
                api.listIndexes().indexes.forEach { api.deleteIndex(it) }
                vectors.deleteBucket(bucket)
            }
        }
        createdBuckets.clear()
    }

}
