package vectors.data

import io.supabase.storage.vectors.data.QueryVectorsOptions
import io.supabase.storage.vectors.data.VectorData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueryVectorsOptionsTest {

    private val bucketName = "test-bucket"
    private val indexName = "test-index"

    @Test
    fun testBuildWithRequiredFields() {
        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f, 3.0f))
        }

        val json = options.build()
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))

        val queryVectorArray = json["queryVector"]?.jsonObject?.get("float32")?.jsonArray
        assertEquals(3, queryVectorArray?.size)
        assertEquals(1.0f, queryVectorArray?.get(0)?.toString()?.toFloat())
        assertEquals(2.0f, queryVectorArray?.get(1)?.toString()?.toFloat())
        assertEquals(3.0f, queryVectorArray?.get(2)?.toString()?.toFloat())
    }

    @Test
    fun testBuildWithTopK() {
        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
            topK = 10
        }

        val json = options.build()
        assertEquals(10, json["topK"]?.toString()?.toInt())
    }

    @Test
    fun testBuildWithFilter() {
        val filterJson = buildJsonObject {
            put("category", "documents")
            put("status", "active")
        }

        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
            filter = filterJson
        }

        val json = options.build()
        val filterObj = json["filter"]?.jsonObject
        assertEquals("documents", filterObj?.get("category")?.toString()?.trim('"'))
        assertEquals("active", filterObj?.get("status")?.toString()?.trim('"'))
    }

    @Test
    fun testBuildWithReturnDistance() {
        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
            returnDistance = true
        }

        val json = options.build()
        assertEquals(true, json["returnDistance"]?.toString()?.toBoolean())
    }

    @Test
    fun testBuildWithReturnMetadata() {
        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
            returnMetadata = false
        }

        val json = options.build()
        assertEquals(false, json["returnMetadata"]?.toString()?.toBoolean())
    }

    @Test
    fun testBuildWithAllOptions() {
        val filterJson = buildJsonObject {
            put("tags", buildJsonArray {
                add(Json.encodeToJsonElement("important"))
                add(Json.encodeToJsonElement("urgent"))
            })
        }

        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
            topK = 50
            filter = filterJson
            returnDistance = true
            returnMetadata = true
        }

        val json = options.build()
        assertEquals(bucketName, json["vectorBucketName"]?.toString()?.trim('"'))
        assertEquals(indexName, json["indexName"]?.toString()?.trim('"'))
        assertEquals(50, json["topK"]?.toString()?.toInt())
        assertEquals(true, json["returnDistance"]?.toString()?.toBoolean())
        assertEquals(true, json["returnMetadata"]?.toString()?.toBoolean())

        val filterArray = json["filter"]?.jsonObject?.get("tags")?.jsonArray
        assertEquals(2, filterArray?.size)
        assertEquals("important", filterArray?.get(0)?.toString()?.trim('"'))
        assertEquals("urgent", filterArray?.get(1)?.toString()?.trim('"'))
    }

    @Test
    fun testBuildWithoutOptionalFields() {
        val options = QueryVectorsOptions(bucketName, indexName).apply {
            queryVector = VectorData(floatArrayOf(1.0f, 2.0f))
        }

        val json = options.build()
        assertNull(json["topK"])
        assertNull(json["filter"])
        assertNull(json["returnDistance"])
        assertNull(json["returnMetadata"])
    }

}
