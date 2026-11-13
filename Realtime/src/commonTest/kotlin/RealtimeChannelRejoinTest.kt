import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.realtime.PostgresAction
import io.supabase.realtime.Realtime
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.channel
import io.supabase.realtime.postgresChangeFlow
import io.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Channel errors and rejoins, as in realtime-js (Phoenix's client): a `system` error is reported and the channel stays
// joined; a channel error or a rejected join rejoins by sending the join again.
class RealtimeChannelRejoinTest {

    private val channelId = "channelId"
    private val topic = "realtime:$channelId"

    @Test
    fun testRefusedPostgresChangesReachSystemFlowWithoutRejoining() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        channel.postgresChangeFlow<PostgresAction>("public") { table = "table" }.launchIn(backgroundScope)
        channel.systemFlow().test {
            val (socket, _) = subscribe(channel, sockets)
            assertEquals("ok", awaitItem().status)
            socket.push(refused())
            val error = awaitItem()
            assertEquals("error", error.status)
            assertEquals("postgres_changes", error.extension)
            assertNothingSent(socket)
            assertEquals(RealtimeChannel.Status.SUBSCRIBED, channel.status.value)
        }
    }

    @Test
    fun testRejectedJoinIsRetriedWithoutLeaving() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        channel.subscribe()
        val socket = sockets.created.last()
        socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
        awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
        socket.push(subscribed())
        channel.awaitStatus(RealtimeChannel.Status.SUBSCRIBED)
    }

    @Test
    fun testRejectedJoinsGiveUpAfterMaxAttempts() = runRejoinTest({ maxAttempts = 3 }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.subscribe()
        val socket = sockets.created.last()
        repeat(3) {
            socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
        }
        channel.awaitStatus(RealtimeChannel.Status.UNSUBSCRIBED)
        assertNothingSent(socket)
    }

    @Test
    fun testChannelErrorRejoinsWithoutLeaving() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        val (socket, joinRef) = subscribe(channel, sockets)
        socket.push(channelError(joinRef))
        awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
        socket.push(subscribed())
        channel.awaitStatus(RealtimeChannel.Status.SUBSCRIBED)
    }

    @Test
    fun testCloseForReplacedJoinIsIgnored() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        val (socket, replaced) = subscribe(channel, sockets)
        socket.push(channelError(replaced))
        awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
        // The server closes the join the new one replaced
        socket.push(close(replaced))
        socket.push(subscribed())
        // The close is handled before the join's "ok"
        channel.awaitStatus(RealtimeChannel.Status.SUBSCRIBED)
        assertTrue(client.realtime.subscriptions.containsKey(topic))
        assertEquals(1, sockets.created.size)
    }

    @Test
    fun testSuccessfulJoinResetsAttempts() = runRejoinTest({ maxAttempts = 2 }) { client, sockets ->
        val channel = client.channel(channelId)
        val (socket, firstJoinRef) = subscribe(channel, sockets)
        socket.push(channelError(firstJoinRef))
        val secondJoinRef = awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN).joinRef
        socket.push(subscribed())
        channel.awaitStatus(RealtimeChannel.Status.SUBSCRIBED)
        // Counting starts over after the join succeeded, so this is retried too
        socket.push(channelError(secondJoinRef))
        awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
    }

    private fun runRejoinTest(
        config: Realtime.Config.() -> Unit = {},
        test: suspend TestScope.(SupabaseClient, ScriptedWebsocketFactory) -> Unit,
    ) {
        val sockets = ScriptedWebsocketFactory()
        runTest {
            createTestClient(
                wsHandler = { _, _ -> },
                supabaseHandler = { test(it, sockets) },
                realtimeConfig = {
                    websocketFactory = sockets
                    disconnectOnNoSubscriptions = true
                    disconnectOnEmptyChannelsAfter = Duration.ZERO
                    rejoinDelay = 10.milliseconds
                    config()
                },
            )
        }
    }

    // Subscribes on a new socket and returns it with the join's ref.
    private suspend fun subscribe(channel: RealtimeChannel, sockets: ScriptedWebsocketFactory): Pair<ScriptedWebsocket, String?> {
        channel.subscribe()
        val socket = sockets.created.last()
        val joinRef = awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN).joinRef
        socket.push(subscribed())
        channel.awaitStatus(RealtimeChannel.Status.SUBSCRIBED)
        return socket to joinRef
    }

    private suspend fun awaitSent(socket: ScriptedWebsocket, event: String): RealtimeMessage {
        val message = socket.sent.awaitItem()
        assertEquals(event, message.event)
        return message
    }

    // The library runs on its own dispatcher here, so give it real time to send something
    private suspend fun assertNothingSent(socket: ScriptedWebsocket) {
        withContext(Dispatchers.Default) { delay(300.milliseconds) }
        socket.sent.expectNoEvents()
    }

    private suspend fun RealtimeChannel.awaitStatus(expected: RealtimeChannel.Status) = status.test {
        while(awaitItem() != expected) Unit
        cancelAndIgnoreRemainingEvents()
    }

    private fun subscribed() = system("ok", "system", "Subscribed")

    private fun refused() = system(
        "error",
        "postgres_changes",
        "Unable to subscribe to changes with given parameters. Please check Realtime is enabled for the given connect parameters: [event: *, schema: public, table: table, filters: [], select: nil]",
    )

    private fun system(status: String, extension: String, message: String) =
        RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_SYSTEM, buildJsonObject {
            put("status", status)
            put("extension", extension)
            put("channel", channelId)
            put("message", message)
        }, null)

    private fun rejected(join: RealtimeMessage) = RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_REPLY, buildJsonObject {
        put("status", "error")
        put("response", buildJsonObject { put("reason", "RealtimeDisabledForConfiguration: Unable to subscribe to changes") })
    }, join.ref, join.joinRef)

    // The server sends the join_ref as the ref of these too
    private fun channelError(joinRef: String?) = RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_ERROR, buildJsonObject { }, joinRef, joinRef)

    private fun close(joinRef: String?) = RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_CLOSE, buildJsonObject { }, joinRef, joinRef)

}
