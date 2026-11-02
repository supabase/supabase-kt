import app.cash.turbine.Turbine
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.logging.SupabaseLoggingProcessor
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.RealtimeProtocolVersion
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocket
import io.github.jan.supabase.realtime.websocket.RealtimeWebsocketFactory
import io.ktor.websocket.Frame
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RealtimeLoggingTest {

    @Test
    fun testJoinLogDoesNotContainAccessToken() {
        val logs = LogCapture()
        val sockets = ScriptedWebsocketFactory()
        runLoggingTest(logs, sockets) {
            it.channel("channelId").subscribe()
            val join = sockets.created.last().sent.awaitItem()
            assertEquals(RealtimeChannel.CHANNEL_EVENT_JOIN, join.event)
            assertEquals(EXAMPLE_JWT, join.payload["access_token"]?.jsonPrimitive?.content)
            logs.awaitLog("Subscribing to channel with body")
        }
    }

    @Test
    fun testSendErrorLogDoesNotContainAccessToken() {
        val logs = LogCapture()
        runLoggingTest(logs, FailingSendWebsocketFactory()) {
            it.channel("channelId").subscribe()
            logs.awaitLog("Error while sending ${RealtimeChannel.CHANNEL_EVENT_JOIN} to realtime:channelId")
        }
    }

    private fun runLoggingTest(
        logs: LogCapture,
        sockets: RealtimeWebsocketFactory,
        test: suspend (SupabaseClient) -> Unit,
    ) {
        runTest {
            createTestClient(
                wsHandler = { _, _ -> },
                supabaseHandler = test,
                realtimeConfig = {
                    websocketFactory = sockets
                    jwtToken = EXAMPLE_JWT
                },
                supabaseConfig = {
                    defaultLoggingFactory = { logs }
                },
            )
        }
    }

    private class LogCapture : SupabaseLoggingProcessor {
        private val messages = Turbine<String>(name = "logs")

        override fun isEnabled(level: LogLevel): Boolean = true

        override fun processLog(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
            messages.add(message)
        }

        suspend fun awaitLog(prefix: String): String {
            while(true) {
                val message = messages.awaitItem()
                assertFalse(EXAMPLE_JWT in message, "Access token logged: $message")
                if(message.startsWith(prefix)) return message
            }
        }
    }

    private class FailingSendWebsocketFactory : RealtimeWebsocketFactory {
        override suspend fun create(url: String): RealtimeWebsocket = object : RealtimeWebsocket {
            override val hasIncomingMessages: Boolean = true

            override fun makeRef(): String = "0"

            override suspend fun receive(): Frame = awaitCancellation()

            override suspend fun send(message: RealtimeMessage, vsn: RealtimeProtocolVersion) = throw IllegalStateException("Connection lost")

            override suspend fun send(data: ByteArray) = Unit

            override suspend fun blockUntilDisconnect() = awaitCancellation()

            override fun disconnect() = Unit
        }
    }
}
