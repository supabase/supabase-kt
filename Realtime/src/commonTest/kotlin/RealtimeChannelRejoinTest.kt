import app.cash.turbine.test
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannelBuilder
import io.github.jan.supabase.realtime.RealtimeChannelError
import io.github.jan.supabase.realtime.RealtimeChannelImpl
import io.github.jan.supabase.realtime.RealtimeChannelJoinException
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun testRejectedJoinReachesErrorFlow() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        channel.errorFlow().test {
            channel.subscribe()
            val socket = sockets.created.last()
            socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
            val error = awaitItem()
            assertEquals(RealtimeChannelError.Type.JOIN_REJECTED, error.type)
            assertEquals("RealtimeDisabledForConfiguration: Unable to subscribe to changes", error.reason)
        }
    }

    @Test
    fun testChannelErrorReachesErrorFlow() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        val (socket, joinRef) = subscribe(channel, sockets)
        channel.errorFlow().test {
            socket.push(channelError(joinRef))
            val error = awaitItem()
            assertEquals(RealtimeChannelError.Type.CHANNEL_ERROR, error.type)
            assertNull(error.reason)
        }
    }

    @Test
    fun testErrorThatGivesUpReachesErrorFlow() = runRejoinTest({ maxAttempts = 2 }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.errorFlow().test {
            channel.subscribe()
            val socket = sockets.created.last()
            repeat(2) {
                socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
                assertEquals(RealtimeChannelError.Type.JOIN_REJECTED, awaitItem().type)
            }
        }
        channel.awaitStatus(RealtimeChannel.Status.UNSUBSCRIBED)
    }

    @Test
    fun testFailedPushIsNotAChannelError() = runRejoinTest { client, sockets ->
        val channel = client.channel(channelId)
        val (socket, joinRef) = subscribe(channel, sockets)
        channel.errorFlow().test {
            // The server's error reply to a push on the joined channel, not to its join
            socket.push(RealtimeMessage(topic, RealtimeChannel.CHANNEL_EVENT_REPLY, buildJsonObject {
                put("status", "error")
                put("response", buildJsonObject { put("reason", "unmatched topic") })
            }, "5", joinRef))
            withContext(Dispatchers.Default) { delay(300.milliseconds) }
            expectNoEvents()
        }
    }

    @Test
    fun testUnansweredJoinTimesOutAndIsRetried() = runRejoinTest({ timeout = 200.milliseconds }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.errorFlow().test {
            channel.subscribe()
            val socket = sockets.created.last()
            awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
            val error = awaitItem()
            assertEquals(RealtimeChannelError.Type.TIMED_OUT, error.type)
            assertNull(error.reason)
            assertNull(error.payload)
            awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
            // The retried join times out too
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testTimedOutJoinsGiveUpAfterMaxAttempts() = runRejoinTest({
        maxAttempts = 2
        timeout = 100.milliseconds
    }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.subscribe()
        val socket = sockets.created.last()
        repeat(2) { awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN) }
        channel.awaitStatus(RealtimeChannel.Status.UNSUBSCRIBED)
        assertNothingSent(socket)
    }

    @Test
    fun testAnsweredJoinDoesNotTimeOut() = runRejoinTest({ timeout = 500.milliseconds }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.errorFlow().test {
            val (socket, _) = subscribe(channel, sockets)
            withContext(Dispatchers.Default) { delay(1.seconds) }
            expectNoEvents()
            socket.sent.expectNoEvents()
        }
    }

    @Test
    fun testRejectedJoinDoesNotAlsoTimeOut() = runRejoinTest({
        timeout = 300.milliseconds
        rejoinDelay = 1.seconds
    }) { client, sockets ->
        val channel = client.channel(channelId)
        channel.errorFlow().test {
            channel.subscribe()
            val socket = sockets.created.last()
            socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
            assertEquals(RealtimeChannelError.Type.JOIN_REJECTED, awaitItem().type)
            // The rejoin comes after rejoinDelay, which is longer than the rejected join's timeout
            awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
            expectNoEvents()
            // The new join's own timeout
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Like realtime-js: the server can hold the join until the postgres changes subscription is set up
    @Test
    fun testWaitingForPostgresChangesExtendsTheJoinTimeout() = runRejoinTest({ timeout = 20.seconds }) { client, _ ->
        fun channel(id: String, builder: RealtimeChannelBuilder.() -> Unit = {}, withChanges: Boolean = true): RealtimeChannelImpl {
            val channel = client.channel(id, builder) as RealtimeChannelImpl
            if(withChanges) channel.postgresChangeFlow<PostgresAction>("public") { table = "table" }
            return channel
        }
        assertEquals(20.seconds, channel("plain").joinTimeout())
        // The server's default wait (15 s) plus time for it to reject the join
        assertEquals(25.seconds, channel("waiting", { postgresChangesOptions { wait = true } }).joinTimeout())
        assertEquals(20.seconds, channel("shortWait", {
            postgresChangesOptions {
                wait = true
                timeout = 1.seconds
            }
        }).joinTimeout())
        assertEquals(20.seconds, channel("noChanges", { postgresChangesOptions { wait = true } }, withChanges = false).joinTimeout())
    }

    @Test
    fun testBlockingSubscribeFailsWhenTheChannelGivesUp() = runRejoinTest({ maxAttempts = 2 }) { client, sockets ->
        client.realtime.connect()
        val socket = sockets.created.single()
        val channel = client.channel(channelId)
        val subscribing = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        repeat(2) {
            socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
        }
        val failure = assertIs<RealtimeChannelJoinException>(awaitResult(subscribing).exceptionOrNull())
        assertEquals(RealtimeChannelError.Type.JOIN_REJECTED, failure.error?.type)
        assertEquals("RealtimeDisabledForConfiguration: Unable to subscribe to changes", failure.error?.reason)
    }

    @Test
    fun testBlockingSubscribeFailsWhenTheChannelIsClosed() = runRejoinTest { client, sockets ->
        client.realtime.connect()
        val socket = sockets.created.single()
        val channel = client.channel(channelId)
        val subscribing = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        socket.push(close(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN).joinRef))
        val failure = assertIs<RealtimeChannelJoinException>(awaitResult(subscribing).exceptionOrNull())
        assertNull(failure.error)
    }

    @Test
    fun testBlockingSubscribeCanSucceedAfterGivingUp() = runRejoinTest({ maxAttempts = 1 }) { client, sockets ->
        client.realtime.connect()
        val socket = sockets.created.single()
        val channel = client.channel(channelId)
        val first = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        socket.push(rejected(awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)))
        assertTrue(awaitResult(first).isFailure)
        val second = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN)
        socket.push(subscribed())
        assertTrue(awaitResult(second).isSuccess)
    }

    // Either outcome is fine, as long as subscribe() returns: the close can be handled before it starts waiting
    @Test
    fun testBlockingSubscribeReturnsWhenClosedRightAfterSubscribing() = runRejoinTest { client, sockets ->
        client.realtime.connect()
        val socket = sockets.created.single()
        val channel = client.channel(channelId)
        val subscribing = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        val joinRef = awaitSent(socket, RealtimeChannel.CHANNEL_EVENT_JOIN).joinRef
        socket.push(subscribed())
        socket.push(close(joinRef))
        awaitResult(subscribing)
    }

    // The channel rejoins once the client reconnects
    @Test
    fun testBlockingSubscribeWaitsThroughALostConnection() = runRejoinTest({ reconnectDelay = 10.milliseconds }) { client, sockets ->
        client.realtime.connect()
        val lost = sockets.created.single()
        val channel = client.channel(channelId)
        val subscribing = async(Dispatchers.Default) { runCatching { channel.subscribe(blockUntilSubscribed = true) } }
        awaitSent(lost, RealtimeChannel.CHANNEL_EVENT_JOIN)
        lost.lose()
        withContext(Dispatchers.Default) {
            withTimeoutOrNull(FLOW_TIMEOUT) {
                while(sockets.created.size < 2) delay(10.milliseconds)
            } ?: fail("The client didn't reconnect")
        }
        val replacement = sockets.created[1]
        awaitSent(replacement, RealtimeChannel.CHANNEL_EVENT_JOIN)
        replacement.push(subscribed())
        assertTrue(awaitResult(subscribing).isSuccess)
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

    // The library runs on its own dispatcher here, so wait in real time. withTimeoutOrNull, not withTimeout: its
    // TimeoutCancellationException would only cancel the coroutine createTestClient runs the test in, which passes.
    private suspend fun <T> awaitResult(deferred: Deferred<T>): T = withContext(Dispatchers.Default) {
        withTimeoutOrNull(FLOW_TIMEOUT) { deferred.await() }
    } ?: fail("subscribe() didn't return within $FLOW_TIMEOUT")

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
