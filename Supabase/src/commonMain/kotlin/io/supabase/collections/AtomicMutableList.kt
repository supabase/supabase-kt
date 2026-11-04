package io.supabase.collections

import io.supabase.annotations.SupabaseInternal
import kotlinx.collections.immutable.persistentListOf
import kotlin.concurrent.atomics.AtomicReference

/**
 * A multiplatform, thread-safe [MutableList], implemented using AtomicReference and PersistentList.
 */
@SupabaseInternal
class AtomicMutableList<E>(
    vararg elements: E
) : MutableList<E> {

    private val list = AtomicReference(persistentListOf(*elements))

    override val size: Int
        get() = list.load().size

    override fun clear() {
        list.updateIfChanged { if (it.isEmpty()) it else persistentListOf() }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if(elements.isEmpty()) return false
        return list.updateIfChanged { current ->
            current.addAll(elements)
        }
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if(elements.isEmpty()) return false
        return list.updateIfChanged { current ->
            current.addAll(index, elements)
        }
    }

    override fun add(index: Int, element: E) = list.update { current ->
        current.add(index, element)
    }

    override fun add(element: E): Boolean = list.updateIfChanged { current ->
        current.add(element)
    }

    override fun get(index: Int): E = list.load()[index]

    override fun isEmpty(): Boolean = list.load().isEmpty()

    override fun iterator(): MutableIterator<E> = list.load().toMutableList().iterator()

    override fun listIterator(): MutableListIterator<E> = list.load().toMutableList().listIterator()

    override fun listIterator(index: Int): MutableListIterator<E> = list.load().toMutableList().listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> =
        list.load().subList(fromIndex, toIndex).toMutableList()

    override fun set(index: Int, element: E): E {
        while (true) {
            val cur = list.load()
            checkElementIndex(index, cur.size)
            val old = cur[index]
            val next = cur.set(index, element)
            if (list.compareAndSet(cur, next)) return old
        }
    }

    override fun removeAt(index: Int): E {
        while (true) {
            val cur = list.load()
            checkElementIndex(index, cur.size)
            val old = cur[index]
            val next = cur.removeAt(index)
            if (list.compareAndSet(cur, next)) return old
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean = list.updateIfChanged { current ->
        current.retainAll(elements)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        if(elements.isEmpty()) return false
        return list.updateIfChanged { current ->
            current.removeAll(elements)
        }
    }

    override fun remove(element: E): Boolean = list.updateIfChanged { current ->
        current.remove(element)
    }

    override fun lastIndexOf(element: E): Int = list.load().lastIndexOf(element)

    override fun indexOf(element: E): Int = list.load().indexOf(element)

    override fun containsAll(elements: Collection<E>): Boolean = list.load().containsAll(elements)

    override fun contains(element: E): Boolean = list.load().contains(element)

    /**
     * Copied from Kotlin [AbstractList] internal code.
     */
    private fun checkElementIndex(index: Int, size: Int) {
        if (index !in 0..<size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }
}