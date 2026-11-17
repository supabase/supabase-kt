import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.event.RBroadcastEvent
import io.supabase.realtime.event.RCloseEvent
import io.supabase.realtime.event.RErrorEvent
import io.supabase.realtime.event.RPostgresChangesEvent
import io.supabase.realtime.event.RPostgresServerChangesEvent
import io.supabase.realtime.event.RPresenceDiffEvent
import io.supabase.realtime.event.RPresenceStateEvent
import io.supabase.realtime.event.RSystemEvent
import io.supabase.realtime.event.RSystemReplyEvent
import io.supabase.realtime.event.RTokenExpiredEvent
import io.supabase.realtime.event.RealtimeEvent
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeEventTest {

    @Test
    fun testSystemType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_SYSTEM,
            payload = buildJsonObject {
                put("status", "ok")
            },
            ref = ""
        )
        assertEquals(RSystemEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testSystemErrorType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_SYSTEM,
            payload = buildJsonObject {
                put("status", "error")
            },
            ref = ""
        )
        assertEquals(RSystemEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testRejectedReplyType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_REPLY,
            payload = buildJsonObject {
                put("status", "error")
            },
            ref = ""
        )
        assertEquals(RErrorEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testJoinReplyWithPostgresChangesType() {
        assertEquals(RPostgresServerChangesEvent, RealtimeEvent.resolveEvent(joinReply("ok")))
    }

    @Test
    fun testRejectedJoinReplyWithPostgresChangesType() {
        assertEquals(RErrorEvent, RealtimeEvent.resolveEvent(joinReply("error")))
    }

    @Test
    fun testTokenExpiredSystemErrorType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_SYSTEM,
            payload = buildJsonObject {
                put("status", "error")
                put("message", "access token has expired")
            },
            ref = ""
        )
        assertEquals(RTokenExpiredEvent, RealtimeEvent.resolveEvent(message))
    }

    // resolveEvent relies on this, not on the events' order
    @Test
    fun testEachMessageHasOneEvent() {
        val messages = listOf(
            message(RealtimeChannel.CHANNEL_EVENT_BROADCAST),
            message(RealtimeChannel.CHANNEL_EVENT_CLOSE),
            message(RealtimeChannel.CHANNEL_EVENT_ERROR),
            message(RealtimeChannel.CHANNEL_EVENT_POSTGRES_CHANGES),
            message(RealtimeChannel.CHANNEL_EVENT_PRESENCE_STATE),
            message(RealtimeChannel.CHANNEL_EVENT_PRESENCE_DIFF),
            message(RealtimeChannel.CHANNEL_EVENT_SYSTEM, "ok"),
            message(RealtimeChannel.CHANNEL_EVENT_SYSTEM, "error"),
            message(RealtimeChannel.CHANNEL_EVENT_SYSTEM, "error", "access token has expired"),
            message(RealtimeChannel.CHANNEL_EVENT_SYSTEM, null, "access token has expired"),
            message(RealtimeChannel.CHANNEL_EVENT_REPLY, "ok"),
            message(RealtimeChannel.CHANNEL_EVENT_REPLY, "error"),
            joinReply("ok"),
            joinReply("error"),
            joinReply(null),
        )
        for(message in messages) {
            val events = RealtimeEvent.EVENTS.filter { it.appliesTo(message) }
            assertEquals(1, events.size, "$message matches $events")
        }
    }

    private fun message(event: String, status: String? = null, text: String? = null) = RealtimeMessage(
        topic = "",
        event = event,
        payload = buildJsonObject {
            status?.let { put("status", it) }
            text?.let { put("message", it) }
        },
        ref = ""
    )

    private fun joinReply(status: String?) = RealtimeMessage(
        topic = "",
        event = RealtimeChannel.CHANNEL_EVENT_REPLY,
        payload = buildJsonObject {
            status?.let { put("status", it) }
            put("response", buildJsonObject {
                put(RealtimeChannel.CHANNEL_EVENT_POSTGRES_CHANGES, buildJsonArray { })
            })
        },
        ref = ""
    )

    @Test
    fun testSystemReplyType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_REPLY,
            payload = buildJsonObject {
                put("status", "ok")
            },
            ref = ""
        )
        assertEquals(RSystemReplyEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testPostgresServerChangesType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_REPLY,
            payload = buildJsonObject {
                put("response", buildJsonObject {
                    put(RealtimeChannel.CHANNEL_EVENT_POSTGRES_CHANGES, "")
                })
            },
            ref = ""
        )
        assertEquals(RPostgresServerChangesEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testPostgresChangesType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_POSTGRES_CHANGES,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RPostgresChangesEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testBroadcastType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_BROADCAST,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RBroadcastEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testCloseType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_CLOSE,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RCloseEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testErrorType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_ERROR,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RErrorEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testPresenceDiffType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_PRESENCE_DIFF,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RPresenceDiffEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testPresenceStateType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_PRESENCE_STATE,
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = ""
        )
        assertEquals(RPresenceStateEvent, RealtimeEvent.resolveEvent(message))
    }

    @Test
    fun testTokenExpiredType() {
        val message = RealtimeMessage(
            topic = "",
            event = RealtimeChannel.CHANNEL_EVENT_SYSTEM,
            payload = buildJsonObject {
                put("message", "access token has expired")
            },
            ref = ""
        )
        assertEquals(RTokenExpiredEvent, RealtimeEvent.resolveEvent(message))
    }

}