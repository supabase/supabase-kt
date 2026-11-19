package io.supabase.dsl

import io.supabase.annotations.SupabaseInternal
import kotlin.reflect.KProperty

@SupabaseInternal
class Required<T> {
    private var value: T? = null

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T =
        value ?: error("${prop.name} is required")

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, v: T) {
        value = v
    }
}

@SupabaseInternal
fun <T> required() = Required<T>()