package io.supabase.realtime.websocket

import io.supabase.realtime.RealtimeMessage
import io.supabase.realtime.RealtimeProtocolVersion
import io.supabase.realtime.broadcast.encodeV2Text
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

/**
 * Implementation of [RealtimeWebsocket] using Ktor's [DefaultClientWebSocketSession].
 */
class KtorRealtimeWebsocket(
    private val ws: DefaultClientWebSocketSession,
): RealtimeWebsocket {

    override val hasIncomingMessages: Boolean get() = ws.isActive
    internal val ref = AtomicInt(0)

    override fun makeRef(): String {
        return ref.fetchAndIncrement().toString()
    }

    override suspend fun receive(): Frame {
        return ws.incoming.receive()
    }

    override suspend fun send(message: RealtimeMessage, vsn: RealtimeProtocolVersion) {
        when(vsn) {
            RealtimeProtocolVersion.V1 -> ws.sendSerialized(message)
            RealtimeProtocolVersion.V2 -> ws.send(message.encodeV2Text())
        }
    }

    override suspend fun send(data: ByteArray) {
        ws.send(data)
    }

    override fun disconnect() {
        ws.cancel()
    }

    override suspend fun blockUntilDisconnect() {
        ws.coroutineContext.job.join()
    }

}