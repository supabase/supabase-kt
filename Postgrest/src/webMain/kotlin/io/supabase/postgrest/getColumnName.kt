package io.supabase.postgrest

import io.supabase.annotations.SupabaseInternal
import kotlin.reflect.KProperty1

@SupabaseInternal
actual fun <T, V> getSerialName(property: KProperty1<T, V>) = property.name
