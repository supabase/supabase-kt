package io.supabase.realtime.websocket

import io.supabase.annotations.SupabaseInternal

/**
 * Interface for creating a websocket connection to the Supabase Realtime service.
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(SupabaseInternal::class)
interface RealtimeWebsocketFactory {

    /**
     * Create a new websocket connection to the given URL.
     * @param url The URL to connect to.
     */
    suspend fun create(url: String): RealtimeWebsocket

}