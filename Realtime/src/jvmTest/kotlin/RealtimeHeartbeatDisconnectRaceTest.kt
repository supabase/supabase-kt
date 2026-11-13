import io.supabase.annotations.SupabaseInternal
import io.supabase.createSupabaseClient
import io.supabase.realtime.Realtime
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.RealtimeProtocolVersion
import io.supabase.realtime.realtime
import io.supabase.realtime.websocket.RealtimeWebsocket
import io.supabase.realtime.websocket.RealtimeWebsocketFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Catches potential regression, where sending heartbeats causes an uncaught exception because it was run after the websocket got cancelled
 */
@OptIn(SupabaseInternal::class)
class RealtimeHeartbeatDisconnectRaceTest {

    private class NoopWebsocket : RealtimeWebsocket {
        private val ref = AtomicInteger(0)
        override val hasIncomingMessages: Boolean get() = true
        override fun makeRef(): String = ref.incrementAndGet().toString()
        override suspend fun receive(): Frame = awaitCancellation()
        override suspend fun send(message: RealtimeMessage, vsn: RealtimeProtocolVersion) = Unit
        override suspend fun send(data: ByteArray) = Unit
        override suspend fun blockUntilDisconnect() = awaitCancellation()
        override fun disconnect() = Unit
    }

    private object NoopFactory : RealtimeWebsocketFactory {
        override suspend fun create(url: String): RealtimeWebsocket = NoopWebsocket()
    }

    @Test
    fun heartbeatTickRacingDisconnectMustNotEscapeAsUncaughtException() {
        val escaped = AtomicReference<Throwable?>(null)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> escaped.compareAndSet(null, e) }
        val client = createSupabaseClient("https://example.supabase.co", "anon-key") {
            httpEngine = MockEngine { respond("") }
            coroutineDispatcher = Dispatchers.Default
            install(Realtime) {
                websocketFactory = NoopFactory
                heartbeatInterval = 1.milliseconds
                reconnectDelay = 1.milliseconds
                disconnectOnNoSubscriptions = false
            }
        }
        val start = System.currentTimeMillis()
        try {
            runBlocking { client.realtime.connect() }
            val hammer = Thread {
                while (escaped.get() == null && System.currentTimeMillis() - start < BUDGET_MS) {
                    client.realtime.disconnect()
                    runBlocking { runCatching { client.realtime.connect() } }
                }
            }.apply { start() }
            hammer.join()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
            runBlocking { runCatching { client.close() } }
        }
        val e = escaped.get()
        println("RACE-TEST: escaped=${e?.let { "${it::class.simpleName}: ${it.message}" }} after ${System.currentTimeMillis() - start} ms")
        assertNull(e, "An exception escaped the realtime scope: $e")
    }

    private companion object {
        const val BUDGET_MS = 20_000L
    }
}
