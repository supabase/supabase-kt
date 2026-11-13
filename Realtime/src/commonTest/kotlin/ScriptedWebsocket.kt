import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.RealtimeProtocolVersion
import io.supabase.realtime.websocket.RealtimeWebsocket
import io.supabase.realtime.websocket.RealtimeWebsocketFactory
import io.ktor.websocket.Frame
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlin.concurrent.atomics.AtomicBoolean

/**
 * A new [ScriptedWebsocket] per connection, so a test can disconnect and reconnect.
 */
class ScriptedWebsocketFactory : RealtimeWebsocketFactory {
    val created = mutableListOf<ScriptedWebsocket>()

    override suspend fun create(url: String): RealtimeWebsocket = ScriptedWebsocket().also { created += it }
}

/**
 * A socket the test plays the server for: it reads what the client sent from [sent] and sends with [push].
 */
class ScriptedWebsocket : RealtimeWebsocket {
    private val _sent = Turbine<RealtimeMessage>(name = "sent")
    val sent: ReceiveTurbine<RealtimeMessage> = _sent
    private val incoming = Channel<Frame>(Channel.UNLIMITED)
    private val open = AtomicBoolean(true)
    private var ref = 0

    override val hasIncomingMessages: Boolean
        get() = open.load()

    override suspend fun receive(): Frame = incoming.receive()

    override suspend fun send(message: RealtimeMessage, vsn: RealtimeProtocolVersion) {
        _sent.add(message)
    }

    override suspend fun send(data: ByteArray) = Unit

    override fun makeRef(): String = (ref++).toString()

    override suspend fun blockUntilDisconnect() = awaitCancellation()

    override fun disconnect() {
        open.store(false)
        incoming.close()
    }

    suspend fun push(message: RealtimeMessage) = incoming.send(message.toFrame())

    /** The connection is lost without a close from the client, e.g. while the device sleeps. */
    fun lose() {
        incoming.close(IllegalStateException("Connection lost"))
    }
}
