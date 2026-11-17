import io.supabase.auth.jwt.JWTUtils
import io.supabase.realtime.Realtime
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeImpl
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.RealtimeProtocolVersion
import io.supabase.realtime.channel
import io.supabase.realtime.realtime
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RealtimeTest {

    @Test
    fun testRealtimeStatus() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.disconnect()
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectDelayZero() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    disconnectOnEmptyChannelsAfter = Duration.ZERO
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeChannel(channel)
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectNonEmpty() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    disconnectOnEmptyChannelsAfter = Duration.ZERO
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    val channel2 = it.realtime.channel("test2")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeChannel(channel)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectNonZero() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    coroutineScope = backgroundScope
                    disconnectOnEmptyChannelsAfter = 15.seconds
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeChannel(channel)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    advanceTimeBy(10.seconds)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    advanceTimeBy(6.seconds) // 1 seconds more because its not 100% accurate
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectNonZeroNonEmpty() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    coroutineScope = backgroundScope
                    disconnectOnEmptyChannelsAfter = 2.seconds
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    val channel2 = it.realtime.channel("test2")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeChannel(channel)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    advanceTimeBy(3.seconds)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectNonZeroDefault() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    coroutineScope = backgroundScope
                    heartbeatInterval = 15.seconds
                    // heartbeatInterval is 15 seconds so the disconnect delay should be 30s
                },
                supabaseHandler = {
                    assertEquals(30.seconds, it.realtime.config.disconnectDelay)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectNonZeroCancel() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    coroutineScope = backgroundScope
                    disconnectOnEmptyChannelsAfter = 15.seconds
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeChannel(channel)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    advanceTimeBy(10.seconds)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.channel("test2")
                    advanceTimeBy(6.seconds) // 1 seconds more because its not 100% accurate
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testRealtimeDisconnectRemoveAll() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                realtimeConfig = {
                    disconnectOnEmptyChannelsAfter = 200.seconds
                    disconnectOnNoSubscriptions = true
                },
                supabaseHandler = {
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    val channel = it.realtime.channel("test")
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    it.realtime.removeAllChannels()
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                }
            )
        }
    }


    @Test
    fun testTeardown() {
        runTest {
            createTestClient(
                wsHandler = { r, _ ->
                    r.receive()
                },
                supabaseHandler = {
                    val channel = it.realtime.channel("test")
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    it.realtime.connect()
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                    channel.subscribe()
                    assertEquals(RealtimeChannel.Status.SUBSCRIBING, channel.status.value)
                    it.realtime.disconnect()
                    assertEquals(Realtime.Status.DISCONNECTED, it.realtime.status.value)
                    assertEquals(RealtimeChannel.Status.UNSUBSCRIBED, channel.status.value)
                }
            )
        }
    }

    @Test
    fun testExistingChannelShouldBeReturned() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                supabaseHandler = {
                    val channel = it.realtime.channel("channelId")
                    val channel2 = it.realtime.channel("channelId")
                    assertEquals(channel, channel2)
                }
            )
        }
    }

    @Test
    fun testSendingRealtimeMessagesV2() {
        val expectedMessage = RealtimeMessage(
            topic = "realtimeTopic",
            event = "realtimeEvent",
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = "realtimeRef"
        )
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive().toMessage()
                    assertEquals(expectedMessage, message)
                },
                supabaseHandler = {
                    it.realtime.connect()
                    it.realtime.send(expectedMessage)
                }
            )
        }
    }


    @Test
    fun testSendingRealtimeMessagesV1() {
        val expectedMessage = RealtimeMessage(
            topic = "realtimeTopic",
            event = "realtimeEvent",
            payload = buildJsonObject {
                put("key", "value")
            },
            ref = "realtimeRef"
        )
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive().toMessage(RealtimeProtocolVersion.V1)
                    assertEquals(expectedMessage, message)
                },
                supabaseHandler = {
                    it.realtime.connect()
                    it.realtime.send(expectedMessage)
                },
                realtimeConfig = {
                    vsn = RealtimeProtocolVersion.V1
                }
            )
        }
    }

    @Test
    fun testSendingExpiredToken() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->

                },
                supabaseHandler = {
                    it.realtime as RealtimeImpl
                    val expiredToken = generateToken(Clock.System.now().epochSeconds - 10)
                    it.realtime.setAuth(expiredToken)
                    assertNull((it.realtime as RealtimeImpl).accessToken)
                }
            )
        }
    }

    @Test
    fun testSendingValidToken() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->

                },
                supabaseHandler = {
                    it.realtime as RealtimeImpl
                    val token = generateToken(Clock.System.now().epochSeconds + 10)
                    it.realtime.setAuth(token)
                    assertEquals(token, (it.realtime as RealtimeImpl).accessToken)
                }
            )
        }
    }

    private fun generateToken(exp: Long) = buildString {
        append("${JWTUtils.encodeToBase64Url("{\"alg\":\"HS256\"}")}.")
        append(JWTUtils.encodeToBase64Url(buildJsonObject {
            put("exp", exp)
        }.toString()))
        append(".test")
    }

}