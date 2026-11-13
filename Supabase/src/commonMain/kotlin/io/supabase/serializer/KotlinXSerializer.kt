package io.supabase.serializer

import io.supabase.SupabaseSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * A [SupabaseSerializer] that uses kotlinx.serialization
 */
class KotlinXSerializer(private val json: Json = Json) : SupabaseSerializer {

    override fun <T> encode(type: KType, value: T): String = json.encodeToString(json.serializersModule.serializer(type), value)

    @Suppress("UNCHECKED_CAST")
    override fun <T> decode(type: KType, value: String): T =
        json.decodeFromString(json.serializersModule.serializer(type), value) as T

}