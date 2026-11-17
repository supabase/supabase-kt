package vectors.index

import io.supabase.storage.vectors.index.VectorDataType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorDataTypeTest {

    @Test
    fun testFloat32() {
        assertEquals("float32", VectorDataType.FLOAT32.value)
    }

    @Test
    fun testSerializesToApiValue() {
        assertEquals("\"float32\"", Json.encodeToString(VectorDataType.FLOAT32))
        assertEquals(VectorDataType.FLOAT32, Json.decodeFromString<VectorDataType>("\"float32\""))
    }

}