package io.supabase.auth.jwt

import io.supabase.annotations.SupabaseInternal
import kotlinx.io.bytestring.encode
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.io.encoding.Base64

private const val JWT_PARTS = 3

@SupabaseInternal
data class JWTData(
    val claimsResponse: ClaimsResponse,
    val rawHeader: String,
    val rawPayload: String
)

@SupabaseInternal
object JWTUtils {

    val JWT_JSON_INSTANCE = Json { isLenient = true }
    val BASE64_INSTANCE = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun decodeJwt(jwt: String): JWTData {
        val parts = jwt.split(".")
        require(parts.size == JWT_PARTS) {
            "Invalid JWT structure"
        }

        val header = JWT_JSON_INSTANCE.decodeFromString<JwtHeader>(decodeBase64Url(parts[0]))
        val payload = JwtPayload(JWT_JSON_INSTANCE.decodeFromString<JsonObject>(decodeBase64Url(parts[1])))
        val signature = decodeBase64ByteArray(parts[2])
        return JWTData(
            ClaimsResponse(payload, header, signature),
            parts[0],
            parts[1]
        )
    }

    fun encodeToBase64Url(value: String) = BASE64_INSTANCE.encode(value.encodeToByteString())

    fun decodeBase64Url(value: String) = BASE64_INSTANCE.decode(value).decodeToString()

    fun decodeBase64ByteArray(value: String) = BASE64_INSTANCE.decode(value)

}