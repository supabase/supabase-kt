import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.putJsonObject
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_PRESENCE_DIFF
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_PRESENCE_STATE
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_SYSTEM
import io.github.jan.supabase.realtime.RealtimeJoinPayload
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.RealtimeProtocolVersion
import io.github.jan.supabase.realtime.RealtimeTopic
import io.github.jan.supabase.realtime.broadcast.BinaryKind
import io.github.jan.supabase.realtime.broadcast.BroadcastPayload
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import io.github.jan.supabase.realtime.broadcast.decodeV2Text
import io.github.jan.supabase.realtime.broadcast.encodeBroadcast
import io.github.jan.supabase.realtime.broadcast.encodeV2Text
import io.github.jan.supabase.supabaseJson
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

val FLOW_TIMEOUT = 6.seconds

suspend fun Auth.importAuthTokenValid(token: String) {
    importSession(
        UserSession(
            token,
            "",
            expiresAt = Clock.System.now() + 1.hours,
            expiresIn = 1.hours.inWholeSeconds,
            tokenType = ""
        )
    )
}

suspend fun handleSubscribe(
    incoming: ReceiveChannel<Frame>,
    outgoing: SendChannel<Frame>,
    channelId: String,
    handleJoinPayload: (RealtimeJoinPayload) -> Unit = {}
) {
    val payload = Json.decodeFromJsonElement<RealtimeJoinPayload>(incoming.receive().toMessage().payload)
    handleJoinPayload(payload)
    outgoing.send(
        RealtimeMessage(
            RealtimeTopic.withChannelId(channelId),
            CHANNEL_EVENT_SYSTEM,
            buildJsonObject {
                put("status", "ok")
                put("extension", "system")
                put("channel", channelId)
                put("message", "Subscribed")
            },
            ""
        ).toFrame()
    )
}

suspend fun handleUnsubscribe(
    incoming: ReceiveChannel<Frame>,
    outgoing: SendChannel<Frame>,
    channelId: String
) {
    val message = incoming.receive().toMessage()
    assertEquals(RealtimeTopic.withChannelId(channelId), message.topic)
    outgoing.send(
        RealtimeMessage(
            RealtimeTopic.withChannelId(channelId),
            RealtimeChannel.CHANNEL_EVENT_CLOSE,
            buildJsonObject { },
            ""
        ).toFrame()
    )
}

suspend fun SendChannel<Frame>.sendBroadcast(channelId: String, event: String, message: JsonObject) {
    sendBroadcastPayload(channelId, event, BroadcastPayload.Json(message))
}

suspend fun SendChannel<Frame>.sendBroadcastPayload(channelId: String, event: String, payload: BroadcastPayload) {
    send(
        Frame.Binary(
            false, RealtimeBroadcast(RealtimeTopic.withChannelId(channelId), event, payload).encodeBroadcast(
                null, null,
                false, BinaryKind.USER_BROADCAST
            )
        )
    )
}

internal fun RealtimeMessage.toFrame() = Frame.Text(this.encodeV2Text())

internal fun Frame.toMessage(vsn: RealtimeProtocolVersion = RealtimeProtocolVersion.V2) = if (this is Frame.Text) {
    when (vsn) {
        RealtimeProtocolVersion.V1 -> supabaseJson.decodeFromString(readText())
        RealtimeProtocolVersion.V2 -> readText().decodeV2Text()
    }
} else error("Not a text")

suspend fun SendChannel<Frame>.sendPresence(
    channelId: String,
    joins: Map<String, Presence>,
    leaves: Map<String, Presence>
) {
    send(RealtimeMessage(RealtimeTopic.withChannelId(channelId), CHANNEL_EVENT_PRESENCE_DIFF, buildJsonObject {
        put("joins", transformPresenceMap(joins))
        put("leaves", transformPresenceMap(leaves))
    }, "").toFrame())
}

suspend fun SendChannel<Frame>.sendPresenceState(channelId: String, state: Map<String, Presence>) {
    send(RealtimeMessage(RealtimeTopic.withChannelId(channelId), CHANNEL_EVENT_PRESENCE_STATE, transformPresenceMap(state) as JsonObject, "").toFrame())
}

suspend fun SendChannel<Frame>.sendSystem(channelId: String, extension: String, message: String, status: String) {
    send(RealtimeMessage(RealtimeTopic.withChannelId(channelId), CHANNEL_EVENT_SYSTEM, buildJsonObject {
        put("extension", extension)
        put("message", message)
        put("status", status)
        put("channel", channelId)
    }, "").toFrame())
}

fun transformPresenceMap(map: Map<String, Presence>): JsonElement {
    return Json.encodeToJsonElement(map.mapValues {
        buildJsonObject {
            put("metas", buildJsonArray {
                add(buildJsonObject {
                    put("phx_ref", it.value.presenceRef)
                    putJsonObject(it.value.state)
                })
            })
        }
    })
}

suspend inline fun <reified T> Flow<T>.waitForValue(value: T) = filter { it == value }.first()