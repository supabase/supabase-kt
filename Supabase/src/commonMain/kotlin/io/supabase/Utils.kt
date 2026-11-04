package io.supabase

import io.supabase.annotations.SupabaseInternal
import io.supabase.exceptions.SupabaseEncodingException
import io.supabase.logging.i
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.decodeFromJsonElement

@SupabaseInternal
val supabaseJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

@OptIn(ExperimentalSerializationApi::class)
@SupabaseInternal
suspend inline fun <reified T> HttpResponse.safeBody(context: String? = null): T {
    val text = bodyAsText()
    val contextMessage = if(context != null) " in $context" else ""
    return try {
        supabaseJson.decodeFromString(text)
    } catch(e: MissingFieldException) {
        throw SupabaseEncodingException("Couldn't decode payload$contextMessage as ${T::class.simpleName}. Input: ${text.replace("\n", "")}")
    }
}

@SupabaseInternal
fun String.toJsonObject(): JsonObject = supabaseJson.decodeFromString(this)

@SupabaseInternal
fun JsonObjectBuilder.putJsonObject(jsonObject: JsonObject) {
    for (key in jsonObject.keys) {
        put(key, jsonObject[key]!!)
    }
}

@SupabaseInternal
inline fun <reified T> JsonObject.decodeIfNotEmptyOrDefault(default: T): T {
    return if(isEmpty()) {
        default
    } else {
        supabaseJson.decodeFromJsonElement<T>(this)
    }
}

@SupabaseInternal
suspend inline fun <reified T> SupabaseClient.bodyOrNull(response: HttpResponse): T? {
    val text = response.bodyAsText()
    return try {
        supabaseJson.decodeFromString<T>(text)
    } catch(e: SerializationException) {
        logger.i(e) { "Could not decode ${text.take(200)} as ${T::class.simpleName}." }
        null
    }
}