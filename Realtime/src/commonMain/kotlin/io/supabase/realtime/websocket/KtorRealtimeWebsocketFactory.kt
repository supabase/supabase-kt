package io.supabase.realtime.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession

/**
 * Implementation of [RealtimeWebsocketFactory] using Ktor's [HttpClient].
 */
class KtorRealtimeWebsocketFactory(
    private val httpClient: HttpClient,
    ): RealtimeWebsocketFactory {

    override suspend fun create(url: String): RealtimeWebsocket {
        val ws = httpClient.webSocketSession(url)
        return KtorRealtimeWebsocket(ws)
    }

}