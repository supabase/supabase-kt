import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.broadcast.BinaryKind
import io.supabase.realtime.broadcast.BroadcastPayload
import io.supabase.realtime.broadcast.PayloadEncoding
import io.supabase.realtime.broadcast.RealtimeBroadcast
import io.supabase.realtime.broadcast.decodeBinaryPayload
import io.supabase.realtime.broadcast.decodeV2Text
import io.supabase.realtime.broadcast.encodeBroadcast
import io.supabase.realtime.broadcast.encodeV2Text
import io.supabase.supabaseJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RealtimeSerializerTest {

    @Test
    fun testEncodeV2Text() {
        val message = RealtimeMessage(
            "topic",
            "event",
            buildJsonObject {
                put("key", "value")
            },
            "1",
            "2"
        )
        assertEquals("""
            ["2","1","topic","event",{"key":"value"}]
        """.trimIndent(), message.encodeV2Text())
    }

    @Test
    fun testDecodeV2Text() {
        val message = RealtimeMessage(
            "topic",
            "event",
            buildJsonObject {
                put("key", "value")
            },
            "1",
            "2"
        )
        assertEquals(message, """
            ["2","1","topic","event",{"key":"value"}]
        """.trimIndent().decodeV2Text())
    }

    @Test
    fun testV1JoinRefUsesPhoenixSnakeCaseKey() {
        val message = RealtimeMessage(
            "topic",
            "event",
            buildJsonObject {
                put("key", "value")
            },
            "1",
            "2"
        )
        val json = supabaseJson.encodeToString(message)
        assertTrue(json.contains("\"join_ref\":\"2\""))
        assertEquals(message, supabaseJson.decodeFromString<RealtimeMessage>(json))
    }

    @Test
    fun testEncodeAndDecodeBinaryPayload() {
        val originalPayload = byteArrayOf(10, 20, 30)
        val broadcast = RealtimeBroadcast(
            topic = "room1",
            event = "shout",
            payload = BroadcastPayload.Binary(originalPayload)
        )

        val topicBytes = "room1".encodeToByteArray()
        val eventBytes = "shout".encodeToByteArray()

        val header = byteArrayOf(
            BinaryKind.USER_BROADCAST.value,
            topicBytes.size.toByte(),
            eventBytes.size.toByte(),
            0,
            PayloadEncoding.BINARY.value
        )
        val mockIncomingFrame = header + topicBytes + eventBytes + originalPayload

        val decoded = mockIncomingFrame.decodeBinaryPayload()

        assertEquals("room1", decoded.topic)
        assertEquals("shout", decoded.event)
        assertTrue(decoded.payload is BroadcastPayload.Binary)
        assertContentEquals(originalPayload, (decoded.payload as BroadcastPayload.Binary).data)
    }

    @Test
    fun testDecodeJsonPayload() {
        val jsonString = """{"msg":"hello"}"""
        val topicBytes = "chat".encodeToByteArray()
        val eventBytes = "msg".encodeToByteArray()

        val header = byteArrayOf(
            BinaryKind.USER_BROADCAST.value,
            topicBytes.size.toByte(),
            eventBytes.size.toByte(),
            0,
            PayloadEncoding.JSON.value
        )
        val mockIncomingFrame = header + topicBytes + eventBytes + jsonString.encodeToByteArray()

        val decoded = mockIncomingFrame.decodeBinaryPayload()

        assertEquals("chat", decoded.topic)
        assertEquals("msg", decoded.event)
        assertTrue(decoded.payload is BroadcastPayload.Json)
        assertEquals("hello", (decoded.payload as BroadcastPayload.Json).value.jsonObject["msg"]?.jsonPrimitive?.content)
    }

    @Test
    fun testEncodeBroadcastStructure() {
        val broadcast = RealtimeBroadcast(
            topic = "test",
            event = "test",
            payload = BroadcastPayload.Binary(byteArrayOf(99))
        )

        val encoded = broadcast.encodeBroadcast(
            joinRef = "j1",
            ref = "r1"
        )

        assertEquals(BinaryKind.USER_BROADCAST_PUSH.value, encoded[0])
        assertEquals(2, encoded[1])
        assertEquals(2, encoded[2])
        assertEquals(4, encoded[3])
        assertEquals(4, encoded[4])
        assertEquals(0, encoded[5])
        assertEquals(PayloadEncoding.BINARY.value, encoded[6])
    }

    @Test
    fun testDecodeFrameTooShortThrows() {
        val shortFrame = byteArrayOf(4, 1, 1)
        assertFailsWith<IllegalStateException> {
            shortFrame.decodeBinaryPayload()
        }
    }

    @Test
    fun testDecodeWrongKindThrows() {
        val wrongKindFrame = byteArrayOf(99, 0, 0, 0, 0)
        assertFailsWith<IllegalStateException> {
            wrongKindFrame.decodeBinaryPayload()
        }
    }

}