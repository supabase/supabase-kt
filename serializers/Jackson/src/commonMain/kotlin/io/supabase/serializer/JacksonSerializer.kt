package io.supabase.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.supabase.SupabaseSerializer
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

/**
 * A [SupabaseSerializer] that uses jackson-module-kotlin
 */
class JacksonSerializer(private val mapper: ObjectMapper = jacksonObjectMapper()) : SupabaseSerializer {

    override fun <T> encode(type: KType, value: T): String = mapper.writeValueAsString(value)

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> decode(type: KType, value: String): T = mapper.readValue(value, mapper.constructType(type.javaType)) as T

}