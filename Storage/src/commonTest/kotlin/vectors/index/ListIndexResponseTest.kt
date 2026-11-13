package vectors.index

import io.supabase.storage.vectors.index.ListIndexesResponse
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals

class ListIndexResponseTest {

    @Test
    fun testIndexProperty() {
        val response = ListIndexesResponse(buildJsonArray {
            add(buildJsonObject {
                put("indexName", "nameA")
            })
            add(buildJsonObject {
                put("indexName", "nameB")
            })
            add(buildJsonObject {
                put("indexName", "nameC")
            })
        })
        assertContentEquals(listOf("nameA", "nameB", "nameC"), response.indexes)
    }

}