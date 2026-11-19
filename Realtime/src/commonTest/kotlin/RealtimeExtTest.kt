import app.cash.turbine.test
import io.supabase.postgrest.Postgrest
import io.supabase.realtime.CallbackManager
import io.supabase.realtime.PostgresAction
import io.supabase.realtime.PostgresJoinConfig
import io.supabase.realtime.Presence
import io.supabase.realtime.channel
import io.supabase.realtime.postgresListDataFlow
import io.supabase.realtime.postgresSingleDataFlow
import io.supabase.realtime.presenceDataFlow
import io.supabase.serializer.KotlinXSerializer
import io.supabase.testing.pathAfterVersion
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@Serializable
data class DummyData(val key: Int, val content: String = "")

class RealtimeExtTest {

    //The way this works is that we trigger presence events via the callback manager and then check if the flow receives the correct data
    @Test
    fun testPresenceDataFlow() {
        runTest {
            val firstPresenceList = listOf(
                DummyData(0),
                DummyData(1),
                DummyData(2),
            )
            val secondPresenceList = listOf(
                DummyData(0),
                DummyData(1),
                DummyData(3),
            )
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, "channelId") {
                        assertTrue { it.config.presence.enabled }
                    }
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    val broadcastFlow = channel.presenceDataFlow<DummyData>()
                    broadcastFlow.test(FLOW_TIMEOUT) {
                        channel.subscribe(true)
                        channel.callbackManager.triggerPresence(firstPresenceList, emptyList())
                        assertContentEquals(firstPresenceList, awaitItem())
                        channel.callbackManager.triggerPresence(listOf(DummyData(3)), listOf(DummyData(2)))
                        assertContentEquals(secondPresenceList, awaitItem())
                        channel.callbackManager.triggerPresence(emptyList(), secondPresenceList)
                        assertContentEquals(emptyList(), awaitItem())
                    }
                }
            )
        }
    }

    @Test
    fun testPostgresListDataFlow() { //Maybe there is a better way to test this
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, "channelId") {
                        assertEquals("id=eq.0,age=lt.58", it.config.postgresChanges.first().filter)
                    }
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    channel.callbackManager.setServerChanges(
                        listOf(
                            PostgresJoinConfig(
                                "public",
                                "table",
                                "id=eq.0,age=lt.58",
                                "*",
                                0
                            )
                        )
                    )
                    val dataFlow = channel.postgresListDataFlow(
                        "public",
                        "table",
                        primaryKey = DummyData::key
                    ) {
                        eq("id", 0)
                        lt("age", 58)
                    }
                    dataFlow.test(FLOW_TIMEOUT) {
                        assertContentEquals(listOf(DummyData(0, "first")), awaitItem()) //1. Initial data
                        channel.subscribe(true)
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Update>(DummyData(0, "second"), DummyData(0, "first")) //2.
                        assertContentEquals(listOf(DummyData(0, "second")), awaitItem()) //2.
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Insert>(DummyData(1, "third"), null) //3.
                        assertContentEquals(listOf(DummyData(0, "second"), DummyData(1, "third")), awaitItem())//3.
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Update>(DummyData(0, "fourth"), DummyData(0, "second")) //4.
                        assertContentEquals(listOf(DummyData(0, "fourth"), DummyData(1, "third")), awaitItem())//4.
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Delete>(null, DummyData(1)) //5.
                        assertContentEquals(listOf(DummyData(0, "fourth")), awaitItem()) //5.
                    }
                },
                mockEngineHandler = {
                    assertEquals("/table", it.url.pathAfterVersion())
                    val andFilter = it.url.parameters["and"]
                    assertEquals("(id.eq.0,age.lt.58)", andFilter)
                    respond(Json.encodeToJsonElement(listOf(DummyData(0, "first"))).toString()) //1.
                },
                supabaseConfig = {
                    install(Postgrest)
                }
            )
        }
    }

    @Test
    fun testPostgresSingleDataFlow() { //Maybe there is a better way to test this
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, "channelId")
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    // Set server changes before creating the data flow to avoid race conditions
                    channel.callbackManager.setServerChanges(
                        listOf(
                            PostgresJoinConfig(
                                "public",
                                "table",
                                "key=eq.0",
                                "*",
                                0
                            )
                        )
                    )
                    val dataFlow = channel.postgresSingleDataFlow("public", "table", primaryKey = DummyData::key) {
                        eq("id", 0)
                    }
                    dataFlow.test(FLOW_TIMEOUT) {
                        assertEquals(DummyData(0, "content"), awaitItem()) //1.
                        channel.subscribe(true)
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Update>(DummyData(0, "content4"), DummyData(0, "content")) //2.
                        assertEquals(DummyData(0, "content4"), awaitItem()) //2.
                        channel.callbackManager.triggerPostgresChange<PostgresAction.Delete>(null, DummyData(0)) //3.
                        awaitComplete() //The flow will complete after the deletion
                    }
                },
                mockEngineHandler = {
                    assertEquals("/table", it.url.pathAfterVersion())
                    val urlFilter = it.url.parameters["id"]
                    assertEquals("eq.0", urlFilter)
                    respond(Json.encodeToJsonElement(DummyData(0, "content")).toString()) //1.
                },
                supabaseConfig = {
                    install(Postgrest)
                }
            )
        }
    }

    private inline fun <reified T: PostgresAction> CallbackManager.triggerPostgresChange(
        record: DummyData?,
        oldRecord: DummyData?
    ) {
        when(T::class) {
            PostgresAction.Insert::class -> triggerPostgresChange(listOf(0), PostgresAction.Insert(encode(record), listOf(), Clock.System.now(), KotlinXSerializer()))
            PostgresAction.Update::class -> triggerPostgresChange(listOf(0), PostgresAction.Update(encode(record), encode(oldRecord), listOf(), Clock.System.now(), KotlinXSerializer()))
            PostgresAction.Delete::class -> triggerPostgresChange(listOf(0), PostgresAction.Delete(encode(oldRecord), listOf(), Clock.System.now(), KotlinXSerializer()))
            else -> error("Unknown event type ${T::class}")
        }
    }

    private fun encode(record: DummyData?) = Json.encodeToJsonElement(record).jsonObject

    private fun CallbackManager.triggerPresence(
        joins: List<DummyData>,
        leaves: List<DummyData>
    ) {
        val joinsMap = joins.associateBy { it.key.toString() }.mapValues { Presence(it.key, Json.encodeToJsonElement(it.value).jsonObject) }
        val leavesMap = leaves.associateBy { it.key.toString() }.mapValues { Presence(it.key, Json.encodeToJsonElement(it.value).jsonObject) }
        triggerPresenceDiff(joinsMap, leavesMap)
    }

}