@file:Suppress("MagicNumber")
package io.supabase.realtime.broadcast

import io.supabase.realtime.RealtimeMessage
import io.supabase.supabaseJson
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal enum class BinaryKind(val value: Byte) {
    USER_BROADCAST_PUSH(3),
    USER_BROADCAST(4)
}

internal enum class PayloadEncoding(val value: Byte) {
    BINARY(0),
    JSON(1);

    companion object {
        fun from(value: Byte) = entries.find { it.value == value }
    }
}

internal fun ByteArray.decodeBinaryPayload(): RealtimeBroadcast {
    if(size < 5) error("Binary frame too short: $size bytes")
    val kind = this[0]
    if(kind != BinaryKind.USER_BROADCAST.value) error("Unexpected frame kind: $kind, expected ${BinaryKind.USER_BROADCAST.value}")
    val topicLen = this[1].toInt() and 0xFF
    val eventLen = this[2].toInt() and 0xFF
    val metaLen = this[3].toInt() and 0xFF
    val encoding = PayloadEncoding.from(this[4]) ?: error("Unknown payload encoding: ${this[4]}")
    val headerSize = 5
    val expectedMinSize = headerSize+topicLen+eventLen+metaLen
    if(size < expectedMinSize) error("Payload too short for declared field lengths")
    var offset = headerSize
    val topic = slice(offset..<offset+topicLen).toByteArray().decodeToString()
    offset += topicLen
    val event = slice(offset..<offset+eventLen).toByteArray().decodeToString()
    offset += eventLen
    offset += metaLen
    val payload = slice(offset..<size).toByteArray()
    return when(encoding) {
        PayloadEncoding.BINARY -> RealtimeBroadcast(topic, event, BroadcastPayload.Binary(payload))
        PayloadEncoding.JSON -> RealtimeBroadcast(topic, event, BroadcastPayload.Json(supabaseJson.decodeFromString(payload.decodeToString())))
    }
}

@Suppress("ComplexCondition")
internal fun RealtimeBroadcast.encodeBroadcast(
    joinRef: String?,
    ref: String?,
    encodeRefs: Boolean = true,
    kind: BinaryKind = BinaryKind.USER_BROADCAST_PUSH
): ByteArray {
    val joinRefBytes = (joinRef ?: "").encodeToByteArray()
    val refBytes = (ref ?: "").encodeToByteArray()
    val topicBytes = topic.encodeToByteArray()
    val eventBytes = event.encodeToByteArray()
    val metaBytes = ByteArray(0)
    val (encoding, payload) = when(payload) {
        is BroadcastPayload.Binary -> PayloadEncoding.BINARY to payload.data
        is BroadcastPayload.Json -> PayloadEncoding.JSON to payload.value.toString().encodeToByteArray()
    }

    if(joinRefBytes.size > 255 || refBytes.size > 255 || topicBytes.size > 255 || eventBytes.size > 255 || metaBytes.size > 255)
        error("Binary frame header fields must not exceed 255 bytes each")

    return buildPacket {
        writeByte(kind.value)
        if(encodeRefs) writeByte(joinRefBytes.size.toByte())
        if(encodeRefs) writeByte(refBytes.size.toByte())
        writeByte(topicBytes.size.toByte())
        writeByte(eventBytes.size.toByte())
        writeByte(metaBytes.size.toByte())
        writeByte(encoding.value)
        if(encodeRefs) write(joinRefBytes)
        if(encodeRefs) write(refBytes)
        write(topicBytes)
        write(eventBytes)
        write(metaBytes)
        write(payload)
    }.readByteArray()
}

/**
 * Decodes a JSON array string `[joinRef, ref, topic, event, payload]` into a [RealtimeMessage].
 */
internal fun String.decodeV2Text(): RealtimeMessage {
    val array = Json.decodeFromString<JsonArray>(this)
    if(array.size < 5) error("Expected array with 5 elements, got ${array.size}")
    val joinRef = array[0].jsonPrimitive.contentOrNull
    val ref = array[1].jsonPrimitive.contentOrNull
    val topic = array[2].jsonPrimitive.contentOrNull ?: error("Expected string for topic at index 2")
    val event = array[3].jsonPrimitive.contentOrNull ?: error("Expected string for event at index 3")
    val payload = array.getOrNull(4)?.jsonObject ?: error("Expected object for payload at index 4")
    return RealtimeMessage(topic, event, payload, ref, joinRef)
}

/**
 * Encodes a [RealtimeMessage] as a JSON array string: `[joinRef, ref, topic, event, payload]`.
 */
internal fun RealtimeMessage.encodeV2Text(): String = buildJsonArray {
    add(joinRef)
    add(ref)
    add(topic)
    add(event)
    add(payload)
}.toString()