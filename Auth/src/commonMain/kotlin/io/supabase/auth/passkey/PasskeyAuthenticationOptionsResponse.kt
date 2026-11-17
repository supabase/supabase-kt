package io.supabase.auth.passkey

import io.supabase.serializer.UnixTimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/**
 * The response for [AuthPasskeyApi.startAuthentication]
 * @param challengeId The challenge id
 * @param options Server options
 * @param expiresAt When the authentication session expires
 */
@Serializable
data class PasskeyAuthenticationOptionsResponse(
    @SerialName("challenge_id") val challengeId: String,
    val options: JsonObject,
    @SerialName("expires_at") @Serializable(UnixTimestampSerializer::class) val expiresAt: Instant
)
