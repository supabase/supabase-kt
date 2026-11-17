@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")
package io.supabase.auth.jwt

import io.supabase.auth.decodeValue
import io.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.supabase.auth.optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Instant

/**
 * JWT Payload containing claims for Supabase authentication tokens.
 *
 * Required claims are iss, aud, exp, iat, sub, role, aal, session_id.
 * All other claims are optional as they can be customized via Custom Access Token Hooks.
 *
 * see https://supabase.com/docs/guides/auth/jwt-fields
 */
data class ClaimsResponse(
    val claims: JwtPayload,
    val header: JwtHeader,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClaimsResponse

        if (claims != other.claims) return false
        if (header != other.header) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = claims.hashCode()
        result = 31 * result + header.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
data class JwtHeader(
    val alg: Algorithm,
    val kid: String? = null,
    val typ: String? = null
) {
    enum class Algorithm {
        RS256, ES256, HS256
    }
}

class JwtPayload(
    val claims: JsonObject,
) {

    // required claims
    val iss: String? by claims.optional
    val sub: String? by claims.optional
    val aud: List<String>? = when(val value = claims["aud"]) {
        is JsonArray -> Json.decodeFromJsonElement(value)
        is JsonPrimitive -> listOf(Json.decodeFromJsonElement(value))
        else -> null
    }
    val exp: Instant? by lazy { claims.decodeValue<Long>("exp")?.let { Instant.fromEpochSeconds(it) } }
    val iat: Instant? by lazy { claims.decodeValue<Long>("iat")?.let { Instant.fromEpochSeconds(it) } }
    val role: String? by claims.optional
    val aal: AuthenticatorAssuranceLevel? by claims.optional
    val sessionId: String? by claims.optional.withKey("session_id")

    // standard optional claims
    val email: String? by claims.optional
    val phone: String? by claims.optional
    val isAnonymous: Boolean? by claims.optional.withKey("is_anonymous")

    // optional claims
    val jti: String? by claims.optional
    val nbf: Instant? by lazy { claims.decodeValue<Long>("nbf")?.let { Instant.fromEpochSeconds(it) } }
    val appMetadata: JsonObject? by claims.optional.withKey("app_metadata")
    val userMetadata: JsonObject? by claims.optional.withKey("user_metadata")
    val amr: List<AMREntry>? by claims.optional

    // special claims
    val ref: String? by claims.optional

    inline fun <reified T> getClaimOrNull(key: String): T? = claims.decodeValue(key, Json)

    inline fun <reified T> getClaim(key: String) = getClaimOrNull<T>(key) ?: error("Param not found")

}