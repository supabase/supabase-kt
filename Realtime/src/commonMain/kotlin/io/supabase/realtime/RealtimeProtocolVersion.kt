package io.supabase.realtime

/**
 * Phoenix protocol version used for WebSocket communication.
 * @param value The raw value
 */
enum class RealtimeProtocolVersion(val value: String) {
    /**
     * Protocol 1.0.0 — JSON object text frames for all messages.
     */
    V1("1.0.0"),
    /**
     * Protocol 2.0.0 — JSON array text frames for non-broadcast messages,
     * binary frames for broadcast messages
     */
    V2("2.0.0")
}