package io.supabase.collections

import kotlin.concurrent.atomics.AtomicReference

internal fun <T> AtomicReference<T>.updateIfChanged(callback: (T) -> T): Boolean {
    while(true) {
        val currentValue = load()
        val newValue = callback(currentValue)
        if(currentValue === newValue) return false
        if(compareAndSet(currentValue, newValue)) return true
    }
}

internal fun <T> AtomicReference<T>.update(callback: (T) -> T) {
    while(true) {
        val currentValue = load()
        val newValue = callback(currentValue)
        if(compareAndSet(currentValue, newValue)) return
    }
}