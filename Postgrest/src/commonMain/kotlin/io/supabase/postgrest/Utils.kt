package io.supabase.postgrest

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

private val SNAKE_CASE_REGEX = "([a-z0-9])([A-Z])".toRegex()

@SupabaseInternal
expect fun <T, V> getSerialName(property: KProperty1<T, V>): String

@SupabaseInternal
internal fun String.camelToSnakeCase(): String {
    return this.replace(SNAKE_CASE_REGEX, "$1_$2").lowercase()
}

@PublishedApi internal fun <T> Map<T, List<T>>.mapToFirstValue() = mapValues { it.value.first() }

@OptIn(ExperimentalSerializationApi::class)
@SupabaseInternal
inline fun <reified T> classPropertyNames(): List<String> = serializerOrNull(typeOf<T>())?.descriptor?.elementNames?.toList() ?: throw IllegalArgumentException("Could not find serializer for ${T::class.simpleName}")
