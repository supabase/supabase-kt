@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")
package io.supabase.auth.jwt

import io.supabase.auth.decodeValue
import io.supabase.auth.optional
import io.supabase.auth.withKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class JWK(
    val jwk: JsonObject
) {

    val kty: KeyType by jwk
    val keyOps: List<String> by jwk.withKey("key_ops")
    val alg: String? by jwk.optional
    val kid: String? by jwk.optional

    inline fun <reified T> getParamOrNull(key: String): T? = jwk.decodeValue(key)

    inline fun <reified T> getParam(key: String) = getParamOrNull<T>(key) ?: error("Param not found")

    @Serializable
    enum class KeyType(val value: String) {
        RSA("RSA"), EC("EC"), @SerialName("oct") OCT("oct");
    }

}