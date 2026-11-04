import io.supabase.storage.vectors.DistanceMetric
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceMetricTest {

    @Test
    fun testCosine() {
        assertEquals("cosine", DistanceMetric.COSINE.value)
    }

    @Test
    fun testEuclidean() {
        assertEquals("euclidean", DistanceMetric.EUCLIDEAN.value)
    }

    @Test
    fun testDotProduct() {
        assertEquals("dotproduct", DistanceMetric.DOTPRODUCT.value)
    }

    @Test
    fun testSerializesToApiValue() {
        DistanceMetric.entries.forEach {
            assertEquals("\"${it.value}\"", Json.encodeToString(it))
            assertEquals(it, Json.decodeFromString<DistanceMetric>("\"${it.value}\""))
        }
    }

}