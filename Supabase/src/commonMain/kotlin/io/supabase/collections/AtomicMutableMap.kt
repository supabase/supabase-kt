package io.supabase.collections

import io.supabase.annotations.SupabaseInternal
import kotlinx.collections.immutable.persistentHashMapOf
import kotlin.concurrent.atomics.AtomicReference

/**
 * A multiplatform, thread-safe [MutableMap], implemented using AtomicReference and PersistentMap.
 */
@SupabaseInternal
class AtomicMutableMap<K, V>(
    vararg pairs: Pair<K, V>
) : MutableMap<K, V> {

    private val ref = AtomicReference(persistentHashMapOf(*pairs))

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = ref.load().toMutableMap().entries
    override val keys: MutableSet<K>
        get() = ref.load().toMutableMap().keys
    override val values: MutableCollection<V>
        get() = ref.load().toMutableMap().values

    override val size: Int get() = ref.load().size
    override fun isEmpty(): Boolean = ref.load().isEmpty()

    override fun get(key: K): V? = ref.load()[key]
    override fun containsKey(key: K): Boolean = ref.load().containsKey(key)
    override fun containsValue(value: V): Boolean = ref.load().containsValue(value)

    override fun clear() {
        ref.updateIfChanged { if (it.isEmpty()) it else persistentHashMapOf() }
    }

    override fun putAll(from: Map<out K, V>) {
        if (from.isEmpty()) return
        ref.updateIfChanged { it.putAll(from) }
    }

    override fun put(key: K, value: V): V? {
        while (true) {
            val cur = ref.load()
            val prev = cur[key]
            val next = cur.put(key, value)
            if (next === cur) return prev // same mapping/value, nothing changed
            if (ref.compareAndSet(cur, next)) return prev
        }
    }

    override fun remove(key: K): V? {
        while (true) {
            val cur = ref.load()
            val prev = cur[key] ?: run {
                // still try a no-op remove to confirm no change, but can return fast
                val next = cur.remove(key)
                if (next === cur) return null
                // If structure changed (rare), still CAS to be correct.
                if (ref.compareAndSet(cur, next)) return null
                continue
            }
            val next = cur.remove(key)
            if (ref.compareAndSet(cur, next)) return prev
        }
    }

}