import app.cash.turbine.test
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.jwt.JWTUtils
import io.github.jan.supabase.auth.jwt.JwtHeader
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.CallbackManagerImpl
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresJoinConfig
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeJoinPayload
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.currentPresences
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.util.encodeBase64
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

val EXAMPLE_JWT = buildString {
    append(JWTUtils.encodeToBase64Url(Json.encodeToString(JwtHeader(JwtHeader.Algorithm.HS256))) + ".")
    append(buildJsonObject { put("exp", Clock.System.now().epochSeconds + 500) }.toString().encodeBase64())
    append("." + JWTUtils.encodeToBase64Url("test"))
}

class RealtimeChannelTest {

    @Test
    fun testConnectOnSubscribeDisabled() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                supabaseHandler = {
                    val channel = it.channel("")
                    assertFailsWith<IllegalStateException>() {
                        channel.subscribe()
                    }
                },
                realtimeConfig = {
                    connectOnSubscribe = false
                }
            )
        }
    }

    @Test
    fun testConnectOnSubscribeEnabled() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    i.receive().toMessage()
                },
                supabaseHandler = {
                    val channel = it.channel("")
                    channel.subscribe(false)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testChannelStatusWithoutPostgres() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    i.receive().toMessage()
                    o.sendSystem(channelId, "system", "Subscribed", "ok")
                    handleUnsubscribe(i, o, channelId)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    assertEquals(channel.status.value, RealtimeChannel.Status.UNSUBSCRIBED)
                    assertEquals(it.realtime.status.value, Realtime.Status.DISCONNECTED)
                    channel.subscribe(blockUntilSubscribed = true)
                    assertEquals(channel.status.value, RealtimeChannel.Status.SUBSCRIBED)
                    channel.unsubscribe()
                    assertEquals(channel.status.value, RealtimeChannel.Status.UNSUBSCRIBING)
                    assertEquals(RealtimeChannel.Status.UNSUBSCRIBED, channel.status.waitForValue(RealtimeChannel.Status.UNSUBSCRIBED))
                },
            )
        }
    }

    @Test
    fun testSendingPayloadWithoutJWT() {
        val expectedChannelId = "channelId"
        val expectedIsPrivate = true
        val expectedReceiveOwnBroadcasts = true
        val expectedAcknowledge = true
        val expectedPresenceKey = "presenceKey"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    val message = i.receive().toMessage()
                    val payload = Json.decodeFromJsonElement<RealtimeJoinPayload>(message.payload)
                    assertEquals("realtime:$expectedChannelId", message.topic)
                    assertEquals(expectedIsPrivate, payload.config.isPrivate)
                    assertEquals(expectedReceiveOwnBroadcasts, payload.config.broadcast.receiveOwnBroadcasts)
                    assertEquals(expectedAcknowledge, payload.config.broadcast.acknowledgeBroadcasts)
                    assertEquals(expectedPresenceKey, payload.config.presence.key)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId") {
                        isPrivate = expectedIsPrivate
                        broadcast {
                            receiveOwnBroadcasts = expectedReceiveOwnBroadcasts
                            acknowledgeBroadcasts = expectedAcknowledge
                        }
                        presence {
                            key = expectedPresenceKey
                        }
                    }
                    channel.subscribe()
                }
            )
        }
    }

    @Test
    fun testSendingPayloadWithAuthJWT() {
        val expectedAuthToken = EXAMPLE_JWT
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive().toMessage()
                    assertEquals(expectedAuthToken, message.payload["access_token"]?.jsonPrimitive?.content)
                },
                supabaseHandler = {
                    it.auth.awaitInitialization()
                    it.auth.importAuthTokenValid(expectedAuthToken)
                    val channel = it.channel("channelId")
                    channel.subscribe()
                },
                supabaseConfig = {
                    install(Auth) {
                        minimalConfig()
                    }
                }
            )
        }
    }

    @Test
    fun testSendingPayloadWithCustomJWT() {
        val expectedAuthToken = EXAMPLE_JWT
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive().toMessage()
                    assertEquals(expectedAuthToken, message.payload["access_token"]?.jsonPrimitive?.content)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    channel.subscribe()
                },
                realtimeConfig = {
                    jwtToken = expectedAuthToken
                }
            )
        }
    }

    @Test
    fun testSendingPresenceUnsubscribed() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    assertFailsWith<IllegalStateException> {
                        channel.track(buildJsonObject {  })
                    }
                }
            )
        }
    }

    @Test
    fun testReceivingPresence() {
        val channelId = "channelId"
        val amount = 10
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId) {
                        assertTrue { it.config.presence.enabled }
                    }
                    for(i in 0..amount) {
                        o.sendPresence(
                            channelId,
                            mapOf("userId" to Presence(i.toString(), buildJsonObject { put("key", i) })),
                            mapOf("userId" to Presence((i-1).toString(), buildJsonObject { put("key", i-1) })
                        ))
                    }
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    coroutineScope {
                        launch {
                            val presenceFlow = channel.presenceChangeFlow()
                            presenceFlow.test(FLOW_TIMEOUT) {
                                for(index in 0..amount) {
                                    val value = awaitItem()
                                    val joins =  value.joins
                                    val leaves = value.leaves
                                    assertEquals(1, joins.size)
                                    assertEquals(1, leaves.size)
                                    assertEquals("userId", joins.keys.first())
                                    assertEquals("userId", leaves.keys.first())
                                    assertEquals(index.toString(), joins.values.first().presenceRef)
                                    assertEquals((index-1).toString(), leaves.values.first().presenceRef)
                                    assertEquals(index, joins.values.first().state["key"]?.jsonPrimitive?.int)
                                    assertEquals(index-1, leaves.values.first().state["key"]?.jsonPrimitive?.int)
                                }
                            }
                        }
                        launch {
                            channel.subscribe(true)
                        }
                    }.join()
                }
            )
        }
    }

    @Test
    fun testReceivingSystem() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId) {
                        assertTrue { it.config.broadcast.replicationReady }
                    }
                    o.sendSystem("channelId", "extension", "Message!", "ok")
                },
                supabaseHandler = {
                    val channel = it.channel("channelId") {
                        broadcast {
                            replicationReady = true
                        }
                    }
                    coroutineScope {
                        launch {
                            val systemFlow = channel.systemFlow()
                            systemFlow.test(FLOW_TIMEOUT) {
                                awaitItem() // this is the join payload
                                val payload = awaitItem()
                                assertEquals("channelId", payload.channel)
                                assertEquals("ok", payload.status)
                                assertEquals("Message!", payload.message)
                                assertEquals("extension", payload.extension)
                            }
                        }
                        launch {
                            channel.subscribe(true)
                        }
                    }.join()
                }
            )
        }
    }

    @Test
    fun testSubscribingWithPostgresChanges() {
        val channelId = "channelId"
        val expectedTable = "table"
        val expectedSchema = "public"
        val expectedFilter = "id=eq.1"
        val events = listOf("INSERT", "UPDATE", "DELETE", "*")
        for(event in events) { //Test if all events are correctly handled using the type parameter
            val postgresServerChanges = listOf(PostgresJoinConfig(expectedSchema, expectedTable, expectedFilter, event, 0))
            runTest {
                createTestClient(
                    wsHandler = { i, o ->
                        val message = i.receive().toMessage()
                        assertEquals("realtime:$channelId", message.topic)
                        assertEquals(RealtimeChannel.CHANNEL_EVENT_JOIN, message.event)
                        val payload = Json.decodeFromJsonElement<RealtimeJoinPayload>(message.payload)
                        val postgresChanges = payload.config.postgresChanges
                        assertEquals(1, postgresChanges.size)
                        assertEquals(expectedTable, postgresChanges.first().table)
                        assertEquals(expectedSchema, postgresChanges.first().schema)
                        assertEquals(expectedFilter, postgresChanges.first().filter)
                        assertEquals(event, postgresChanges.first().event)
                        o.send(RealtimeMessage("realtime:$channelId", RealtimeChannel.CHANNEL_EVENT_REPLY, buildJsonObject {
                            put("response", buildJsonObject {
                                put("postgres_changes", Json.encodeToJsonElement(postgresServerChanges))
                            })
                        }, "").toFrame())
                    },
                    supabaseHandler = {
                        val channel = it.channel(channelId)
                        val postgresFlow = channel.flowFromEventType(event, expectedSchema, expectedTable, FilterOperation("id", FilterOperator.EQ, 1))
                        channel.subscribe(true)
                        assertEquals(RealtimeChannel.Status.SUBSCRIBED, channel.status.value)
                        assertContentEquals(postgresServerChanges, (channel.callbackManager as CallbackManagerImpl).serverChanges)
                    }
                )
            }
        }
    }

    @Test
    fun testUnsubscribeIncludesJoinRef() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    val joinMessage = i.receive().toMessage()
                    assertEquals(RealtimeChannel.CHANNEL_EVENT_JOIN, joinMessage.event)
                    val joinRef = joinMessage.joinRef
                    o.sendSystem(channelId, "system", "Subscribed", "ok")
                    val leaveMessage = i.receive().toMessage()
                    assertEquals(RealtimeChannel.CHANNEL_EVENT_LEAVE, leaveMessage.event)
                    assertEquals(joinRef, leaveMessage.joinRef)
                },
                supabaseHandler = {
                    val channel = it.channel(channelId)
                    channel.subscribe(blockUntilSubscribed = true)
                    channel.unsubscribe()
                }
            )
        }
    }

    @Test
    fun testUpdateAuthIncludesJoinRef() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    val joinMessage = i.receive().toMessage()
                    val joinRef = joinMessage.joinRef
                    o.sendSystem(channelId, "system", "Subscribed", "ok")
                    val authMessage = i.receive().toMessage()
                    assertEquals(RealtimeChannel.CHANNEL_EVENT_ACCESS_TOKEN, authMessage.event)
                    assertEquals(joinRef, authMessage.joinRef)
                },
                supabaseHandler = {
                    val channel = it.channel(channelId)
                    channel.subscribe(blockUntilSubscribed = true)
                    channel.updateAuth("newToken")
                }
            )
        }
    }

    @Test
    fun testResubscribeOnPresenceChange() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId) {
                        assertFalse { it.config.presence.enabled }
                    }
                    // Joins again without leaving: the server replaces the join
                    handleSubscribe(i, o, channelId) {
                        assertTrue { it.config.presence.enabled }
                    }
                },
                realtimeConfig = {
                    disconnectOnNoSubscriptions = false
                },
                supabaseHandler = {
                    val channel = it.channel(channelId)
                    channel.status.test {
                        assertEquals(RealtimeChannel.Status.UNSUBSCRIBED, awaitItem())
                        channel.subscribe(false)
                        assertEquals(RealtimeChannel.Status.SUBSCRIBING, awaitItem())
                        assertEquals(RealtimeChannel.Status.SUBSCRIBED, awaitItem())
                        val job = channel.presenceChangeFlow().launchIn(this@runTest)
                        assertEquals(RealtimeChannel.Status.SUBSCRIBING, awaitItem())
                        assertEquals(RealtimeChannel.Status.SUBSCRIBED, awaitItem())
                        job.cancel()
                    }
                }
            )
        }
    }

    @Serializable
    data class PresenceUser(val name: String)

    @Test
    fun testCurrentPresencesUpdatedOnPresenceEvents() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId) {
                        assertTrue { it.config.presence.enabled }
                    }
                    // Send presence join for user1 and user2, then leave for user1
                    o.sendPresence(
                        channelId,
                        mapOf(
                            "user1" to Presence("ref1", buildJsonObject { put("name", "Alice") }),
                            "user2" to Presence("ref2", buildJsonObject { put("name", "Bob") })
                        ),
                        mapOf()
                    )
                    o.sendPresence(
                        channelId,
                        mapOf(),
                        mapOf("user1" to Presence("ref1", buildJsonObject { put("name", "Alice") }))
                    )
                },
                supabaseHandler = {
                    val channel = it.channel(channelId)
                    assertEquals(emptyList(), channel.currentPresences<PresenceUser>())
                    coroutineScope {
                        launch {
                            channel.presenceChangeFlow().test(FLOW_TIMEOUT) {
                                // Consume both presence events (join + leave)
                                awaitItem()
                                awaitItem()
                                // After all events: only user2 should remain
                                val presences = channel.currentPresences<PresenceUser>()
                                assertEquals(1, presences.size)
                                assertEquals("Bob", presences.first().name)
                            }
                        }
                        launch {
                            channel.subscribe(true)
                        }
                    }.join()
                }
            )
        }
    }

    @Test
    fun testPostgresChangesOptionsAreSentWithTheJoin() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val config = i.receive().toMessage().payload["config"]!!.jsonObject
                    assertEquals(buildJsonObject {
                        put("wait", true)
                        put("timeout", 5000)
                    }, config["postgres_changes_options"])
                },
                supabaseHandler = {
                    it.channel("channelId") {
                        postgresChangesOptions {
                            wait = true
                            timeout = 5.seconds
                        }
                    }.subscribe()
                }
            )
        }
    }

    @Test
    fun testNoPostgresChangesOptionsByDefault() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val config = i.receive().toMessage().payload["config"]!!.jsonObject
                    assertFalse(config.containsKey("postgres_changes_options"))
                },
                supabaseHandler = {
                    it.channel("channelId").subscribe()
                }
            )
        }
    }

    //For more complex tests we need integration tests

    private fun RealtimeChannel.flowFromEventType(event: String, schema: String, table: String, filter: FilterOperation): Flow<PostgresAction> {
        when(event) {
            "INSERT" -> return postgresChangeFlow<PostgresAction.Insert>(schema) {
                this.table = table
                filter(filter)
            }
            "UPDATE" -> return postgresChangeFlow<PostgresAction.Update>(schema) {
                this.table = table
                filter(filter)
            }
            "DELETE" -> return postgresChangeFlow<PostgresAction.Delete>(schema) {
                this.table = table
                filter(filter)
            }
            "*" -> return postgresChangeFlow<PostgresAction>(schema) {
                this.table = table
                filter(filter)
            }
            else -> error("Unknown event type $event")
        }
    }

}