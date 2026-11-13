package io.supabase.auth.mfa

import io.supabase.serializer.UnixTimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A challenge to verify the user's identity.
 * @property id The id of the challenge.
 * @property factorType Factor Type which generated the challenge.
 * @property expiresAt Timestamp when this challenge will no longer be usable.
 */
@Serializable
data class MfaChallenge(
    val id: String,
    @SerialName("type") val factorType: String,
    @Serializable(with = UnixTimestampSerializer::class)
    @SerialName("expires_at")
    val expiresAt: Instant
)