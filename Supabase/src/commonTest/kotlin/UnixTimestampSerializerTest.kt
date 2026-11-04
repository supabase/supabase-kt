import io.supabase.serializer.UnixTimestampSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class UnixTimestampSerializerTest {

    @Serializable
    data class Dummy(
        @Serializable(with = UnixTimestampSerializer::class) val someDate: Instant
    )

    @Test
    fun testDecode() {
        val jsonObject = buildJsonObject {
            put("someDate", "1773358025")
        }
        val dummy = Json.decodeFromJsonElement<Dummy>(jsonObject)
        assertEquals("2026-03-12T23:27:05Z", dummy.someDate.toString())
    }

    @Test
    fun testEncode() {
        val dummy = Dummy(Instant.parse("2026-03-12T23:27:05Z"))
        val json = Json.encodeToJsonElement(dummy).jsonObject
        assertEquals("1773358025", json["someDate"]!!.jsonPrimitive.content)
    }

}