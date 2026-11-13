package io.supabase.auth

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.reflect.KProperty

internal fun JsonObjectBuilder.putCaptchaToken(token: String) {
    putJsonObject("gotrue_meta_security") {
        put("captcha_token", token)
    }
}

internal fun JsonObjectBuilder.putCodeChallenge(codeChallenge: String) {
    put("code_challenge", codeChallenge)
    put("code_challenge_method", PKCEConstants.CHALLENGE_METHOD)
}

internal inline operator fun <reified T> JsonObject.getValue(thisRef: Any?, property: KProperty<*>): T {
    return this[property.name]?.let { Json.decodeFromJsonElement(it) } ?: error("No entry found with key ${property.name}")
}

@SupabaseInternal
inline fun <reified T> JsonObject.decodeValue(key: String, json: Json = Json): T? = this[key]?.let { json.decodeFromJsonElement(it) }

internal fun JsonObject.withKey(key: String) = JsonObjectModifier(this, key)

internal val JsonObject.optional get() = OptionalJsonObjectModifier(this)

internal class JsonObjectModifier(
    val json: JsonObject,
    val withKey: String
) {

    val optional get() = OptionalJsonObjectModifier(json, withKey)

    fun withKey(key: String) = JsonObjectModifier(json, key)

    inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T {
        return json[withKey]?.let { Json.decodeFromJsonElement(it) } ?: error("No entry found with key ${property.name}")
    }

}

internal class OptionalJsonObjectModifier(
    val json: JsonObject,
    val withKey: String? = null
) {

    fun withKey(key: String) = OptionalJsonObjectModifier(json, key)

    inline operator fun <reified T> getValue(thisRef: Any?, property: KProperty<*>): T? {
        return json[withKey ?: property.name]?.let {
            if(it is JsonNull) return null
            Json.decodeFromJsonElement(it)
        }
    }

}