import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.realtime.CallbackManagerImpl
import io.github.jan.supabase.realtime.HasRecord
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresJoinConfig
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.RealtimeChannelError
import io.github.jan.supabase.realtime.RealtimeSystemPayload
import io.github.jan.supabase.realtime.broadcast.BroadcastPayload
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock

class CallbackManagerTest {

    @Test
    fun testBroadcastCallbackJson() {
        val cm = CallbackManagerImpl()
        val expectedEvent = "event"
        val expectedPayload = buildJsonObject {
            put("key", "value")
        }
        var called = false
        val id = cm.addBroadcastCallback(expectedEvent) {
            assertIs<BroadcastPayload.Json>(it.payload)
            assertEquals(expectedPayload, it.payload.value)
            called = true
        }
        cm.triggerBroadcast(RealtimeBroadcast("", expectedEvent, BroadcastPayload.Json(expectedPayload)))
        assertTrue { called }
        cm.removeCallbackById(id)
        called = false
        cm.triggerBroadcast(RealtimeBroadcast("", expectedEvent, BroadcastPayload.Json(expectedPayload)))
        assertFalse { called }
    }

    @Test
    fun testSystemCallback() {
        val cm = CallbackManagerImpl()

        var called = false
        val id = cm.addSystemCallback {
            assertEquals("system", it.extension)
            assertEquals("ok", it.status)
            assertEquals("myChannel", it.channel)
            assertEquals("Message!", it.message)
            called = true
        }
        cm.triggerSystem(RealtimeSystemPayload("system", "ok", "Message!", "myChannel"))
        assertTrue { called }
        cm.removeCallbackById(id)
        called = false
        cm.triggerSystem(RealtimeSystemPayload("system", "ok", "Message!", "myChannel"))
        assertFalse { called }
    }

    @Test
    fun testErrorCallback() {
        val cm = CallbackManagerImpl()
        val error = RealtimeChannelError(RealtimeChannelError.Type.JOIN_REJECTED, "reason", buildJsonObject { })

        var called = false
        val id = cm.addErrorCallback {
            assertEquals(error, it)
            called = true
        }
        cm.triggerError(error)
        assertTrue { called }
        cm.removeCallbackById(id)
        called = false
        cm.triggerError(error)
        assertFalse { called }
    }

    @Test
    fun testPresenceCallbacks() {
        val cm = CallbackManagerImpl()
        val expectedJoins = mapOf("join" to Presence("join", buildJsonObject {
            put("key", "value")
        }))
        val expectedLeaves = mapOf("leave" to Presence("leave", buildJsonObject {
            put("key", "value")
        }))
        var called = false
        val id = cm.addPresenceCallback {
            assertEquals(expectedJoins, it.joins)
            assertEquals(expectedLeaves, it.leaves)
            called = true
        }
        assertTrue { cm.hasPresenceCallback() }
        cm.triggerPresenceDiff(expectedJoins, expectedLeaves)
        assertTrue { called }
        cm.removeCallbackById(id)
        called = false
        assertFalse { cm.hasPresenceCallback() }
        cm.triggerPresenceDiff(expectedJoins, expectedLeaves)
        assertFalse { called }
    }

    @Test
    fun testPresenceStateTracksJoins() {
        val cm = CallbackManagerImpl()
        val presence1 = Presence("ref1", buildJsonObject { put("name", "Alice") })
        val presence2 = Presence("ref2", buildJsonObject { put("name", "Bob") })

        assertTrue(cm.presenceState().isEmpty())

        cm.triggerPresenceDiff(mapOf("user1" to presence1), mapOf())
        assertEquals(mapOf("user1" to presence1), cm.presenceState())

        cm.triggerPresenceDiff(mapOf("user2" to presence2), mapOf())
        assertEquals(mapOf("user1" to presence1, "user2" to presence2), cm.presenceState())
    }

    @Test
    fun testPresenceStateTracksLeaves() {
        val cm = CallbackManagerImpl()
        val presence1 = Presence("ref1", buildJsonObject { put("name", "Alice") })
        val presence2 = Presence("ref2", buildJsonObject { put("name", "Bob") })

        cm.triggerPresenceDiff(mapOf("user1" to presence1, "user2" to presence2), mapOf())
        assertEquals(2, cm.presenceState().size)

        cm.triggerPresenceDiff(mapOf(), mapOf("user1" to presence1))
        assertEquals(mapOf("user2" to presence2), cm.presenceState())
    }

    @Test
    fun testPresenceStateHandlesJoinAndLeaveInSameDiff() {
        val cm = CallbackManagerImpl()
        val presence1 = Presence("ref1", buildJsonObject { put("name", "Alice") })
        val presence2 = Presence("ref2", buildJsonObject { put("name", "Bob") })

        cm.triggerPresenceDiff(mapOf("user1" to presence1), mapOf())
        cm.triggerPresenceDiff(mapOf("user2" to presence2), mapOf("user1" to presence1))
        assertEquals(mapOf("user2" to presence2), cm.presenceState())
    }

    @Test
    fun testPresenceStateUpdatesExistingKey() {
        val cm = CallbackManagerImpl()
        val presence1 = Presence("ref1", buildJsonObject { put("name", "Alice") })
        val presence1Updated = Presence("ref1", buildJsonObject { put("name", "Alice Updated") })

        cm.triggerPresenceDiff(mapOf("user1" to presence1), mapOf())
        cm.triggerPresenceDiff(mapOf("user1" to presence1Updated), mapOf())
        assertEquals(mapOf("user1" to presence1Updated), cm.presenceState())
    }

    @Test
    fun testPresenceStateResetOnReset() {
        val cm = CallbackManagerImpl()
        val presence1 = Presence("ref1", buildJsonObject { put("name", "Alice") })

        cm.triggerPresenceDiff(mapOf("user1" to presence1), mapOf())
        assertFalse(cm.presenceState().isEmpty())

        cm.reset()
        assertTrue(cm.presenceState().isEmpty())
    }

    @Test
    fun testPostgresCallbacks() {
        val events = listOf("INSERT", "UPDATE", "DELETE", "*")
        for(event in events) {
            val cm = CallbackManagerImpl()
            val joinConfig = PostgresJoinConfig("public", "table", event = event)
            val expectedRecord = buildJsonObject {
                put("key", "value")
            }
            val expectedOldRecord = buildJsonObject {
                put("key", "oldValue")
            }
            var called = false
            val id = cm.addPostgresCallback(joinConfig) {
                assertTrue { it.shouldBeCalled(event) }
                if(it is HasRecord) {
                    assertEquals(expectedRecord, it.record)
                }
                if(it is PostgresAction.Update) {
                    assertEquals(expectedOldRecord, it.oldRecord)
                }
                called = true
            }
            cm.setServerChanges(listOf(joinConfig))
            cm.triggerPostgresChange(listOf(id.value), actionFromEvent(event, expectedRecord, expectedOldRecord))
            assertTrue { called }
            called = false
            if(event != "*") {
                cm.triggerPostgresChange(listOf(2), actionFromEvent(events.filter { it != event && it != "*" }.random(), expectedRecord, expectedOldRecord))
                assertFalse { called }
            }
            cm.removeCallbackById(id)
            cm.triggerPostgresChange(listOf(id.value), actionFromEvent(event, expectedRecord, expectedOldRecord))
            assertFalse { called }
        }
    }

    private fun PostgresAction.shouldBeCalled(event: String): Boolean {
        return when(this) {
            is PostgresAction.Insert -> event == "INSERT"
            is PostgresAction.Update -> event == "UPDATE"
            is PostgresAction.Delete -> event == "DELETE"
            is PostgresAction.Select -> event == "SELECT"
        } || event == "*"
    }

    private fun actionFromEvent(event: String, record: JsonObject, oldRecord: JsonObject, serializer: SupabaseSerializer = KotlinXSerializer()): PostgresAction {
        return when(event) {
            "INSERT" -> PostgresAction.Insert(record, listOf(), Clock.System.now(), serializer)
            "UPDATE" -> PostgresAction.Update(record, oldRecord, listOf(), Clock.System.now(), serializer)
            "DELETE" -> PostgresAction.Delete(record, listOf(), Clock.System.now(), serializer)
            "SELECT" -> PostgresAction.Select(record, listOf(), Clock.System.now(), serializer)
            "*" -> PostgresAction.Insert(record, listOf(), Clock.System.now(), serializer)
            else -> error("Unknown event $event")
        }
    }

}