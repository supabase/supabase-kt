package io.github.jan.supabase.realtime

import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.realtime.broadcast.RealtimeBroadcast
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.atomics.update

@SupabaseInternal
sealed class RealtimeCallbackId(val value: Int) {

    class Postgres(value: Int) : RealtimeCallbackId(value)

    class Presence(value: Int) : RealtimeCallbackId(value)

    class Broadcast(value: Int) : RealtimeCallbackId(value)

    class System(value: Int) : RealtimeCallbackId(value)

    class Error(value: Int) : RealtimeCallbackId(value)

}

@SupabaseInternal
interface CallbackManager {

    fun triggerPostgresChange(ids: List<Int>, data: PostgresAction)

    fun triggerBroadcast(data: RealtimeBroadcast)

    fun triggerPresenceDiff(joins: Map<String, Presence>, leaves: Map<String, Presence>)

    fun triggerSystem(data: RealtimeSystemPayload)

    fun triggerError(error: RealtimeChannelError)

    fun hasPresenceCallback(): Boolean

    /**
     * Returns the current presence state as a map of keys to [Presence] objects.
     */
    fun presenceState(): Map<String, Presence>

    fun addBroadcastCallback(event: String, callback: (RealtimeBroadcast) -> Unit): RealtimeCallbackId.Broadcast

    fun addSystemCallback(callback: (RealtimeSystemPayload) -> Unit): RealtimeCallbackId.System

    fun addErrorCallback(callback: (RealtimeChannelError) -> Unit): RealtimeCallbackId.Error

    fun addPostgresCallback(filter: PostgresJoinConfig, callback: (PostgresAction) -> Unit): RealtimeCallbackId.Postgres

    fun addPresenceCallback(callback: (PresenceAction) -> Unit): RealtimeCallbackId.Presence

    fun removeCallbackById(id: RealtimeCallbackId)

    fun setServerChanges(changes: List<PostgresJoinConfig>)

    fun reset()

}

private typealias BroadcastMap = PersistentMap<String, PersistentList<RealtimeCallback.BroadcastCallback>>
private typealias SystemMap = PersistentMap<Int, RealtimeCallback.SystemCallback>
private typealias ErrorMap = PersistentMap<Int, RealtimeCallback.ErrorCallback>
private typealias PresenceMap = PersistentMap<Int, RealtimeCallback.PresenceCallback>
private typealias PostgresMap = PersistentMap<Int, RealtimeCallback.PostgresCallback>

internal class CallbackManagerImpl(
    private val serializer: SupabaseSerializer = KotlinXSerializer()
) : CallbackManager {

    private val nextId = AtomicInt(0)
    private val _serverChanges = AtomicReference(listOf<PostgresJoinConfig>())
    val serverChanges: List<PostgresJoinConfig> get() = _serverChanges.load()

    private val currentPresenceState = AtomicReference<PersistentMap<String, Presence>>(persistentHashMapOf())
    private val presenceCallbacks = AtomicReference<PresenceMap>(persistentHashMapOf())

    private val broadcastCallbacks = AtomicReference<BroadcastMap>(persistentHashMapOf())
    // Additional map to know from which list a callback may be removed in broadcastCallbacks without searching through the whole map
    private val broadcastEventId = AtomicReference<PersistentMap<Int, String>>(persistentHashMapOf())

    private val postgresCallbacks = AtomicReference<PostgresMap>(persistentHashMapOf())

    private val systemCallbacks = AtomicReference<SystemMap>(persistentHashMapOf())

    private val errorCallbacks = AtomicReference<ErrorMap>(persistentHashMapOf())

    override fun reset() {
        postgresCallbacks.store(persistentHashMapOf())
        broadcastEventId.store(persistentHashMapOf())
        broadcastCallbacks.store(persistentHashMapOf())
        presenceCallbacks.store(persistentHashMapOf())
        currentPresenceState.store(persistentHashMapOf())
        systemCallbacks.store(persistentHashMapOf())
        errorCallbacks.store(persistentHashMapOf())
        _serverChanges.store(emptyList())
        nextId.store(0)
    }

    override fun addBroadcastCallback(event: String, callback: (RealtimeBroadcast) -> Unit): RealtimeCallbackId.Broadcast {
        val id = nextId.fetchAndIncrement()
        broadcastCallbacks.update {
            val current = it[event] ?: persistentListOf()
            it.putting(event, current + RealtimeCallback.BroadcastCallback(callback, event, id))
        }
        broadcastEventId.update {
            it.putting(id, event)
        }
        return RealtimeCallbackId.Broadcast(id)
    }

    override fun addPostgresCallback(filter: PostgresJoinConfig, callback: (PostgresAction) -> Unit): RealtimeCallbackId.Postgres {
        val id = nextId.fetchAndIncrement()
        postgresCallbacks.update {
            it.putting(id, RealtimeCallback.PostgresCallback(callback, filter, id))
        }
        return RealtimeCallbackId.Postgres(id)
    }

    override fun addSystemCallback(callback: (RealtimeSystemPayload) -> Unit): RealtimeCallbackId.System {
        val id = nextId.fetchAndIncrement()
        systemCallbacks.update {
            it.putting(id, RealtimeCallback.SystemCallback(callback, id))
        }
        return RealtimeCallbackId.System(id)
    }

    override fun triggerSystem(data: RealtimeSystemPayload) {
        systemCallbacks.load().values.forEach { it.callback(data) }
    }

    override fun addErrorCallback(callback: (RealtimeChannelError) -> Unit): RealtimeCallbackId.Error {
        val id = nextId.fetchAndIncrement()
        errorCallbacks.update {
            it.putting(id, RealtimeCallback.ErrorCallback(callback, id))
        }
        return RealtimeCallbackId.Error(id)
    }

    override fun triggerError(error: RealtimeChannelError) {
        errorCallbacks.load().values.forEach { it.callback(error) }
    }

    override fun triggerPostgresChange(ids: List<Int>, data: PostgresAction) {
        val filter = serverChanges.filter { it.id in ids }
        val callbacks =
            postgresCallbacks.load().values.filter { cc -> filter.any { sc -> cc.filter == sc } }
        callbacks.forEach { it.callback(data) }
    }

    override fun triggerBroadcast(data: RealtimeBroadcast) {
        broadcastCallbacks.load()[data.event]?.forEach { it.callback(data) }
    }

    override fun triggerPresenceDiff(joins: Map<String, Presence>, leaves: Map<String, Presence>) {
        currentPresenceState.update { state ->
            var updated = state
            leaves.keys.forEach { key -> updated = updated.remove(key) }
            joins.forEach { (key, presence) -> updated = updated.put(key, presence) }
            updated
        }
        presenceCallbacks.load().values.forEach { it.callback(PresenceActionImpl(serializer, joins, leaves)) }
    }

    override fun presenceState(): Map<String, Presence> = currentPresenceState.load()

    override fun hasPresenceCallback(): Boolean {
        return presenceCallbacks.load().isNotEmpty()
    }

    override fun addPresenceCallback(callback: (PresenceAction) -> Unit):  RealtimeCallbackId.Presence {
        val id = nextId.fetchAndIncrement()
        presenceCallbacks.update {
            it.putting(id, RealtimeCallback.PresenceCallback(callback, id))
        }
        return RealtimeCallbackId.Presence(id)
    }

    fun removeBroadcastCallbackById(id: Int) {
        val event = broadcastEventId.load()[id] ?: return
        broadcastCallbacks.update {
            it.putting(event, it[event]?.removingAll { c -> c.id == id } ?: persistentListOf())
        }
        broadcastEventId.update {
            it.removing(id)
        }
    }

    fun removePresenceCallbackById(id: Int) {
        presenceCallbacks.update {
            it.removing(id)
        }
    }

    fun removePostgresCallbackById(id: Int) {
        postgresCallbacks.update {
            it.removing(id)
        }
    }

    fun removeSystemCallbackById(id: Int) {
        systemCallbacks.update {
            it.removing(id)
        }
    }

    fun removeErrorCallbackById(id: Int) {
        errorCallbacks.update {
            it.removing(id)
        }
    }

    override fun removeCallbackById(id: RealtimeCallbackId) {
        when (id) {
            is RealtimeCallbackId.Broadcast -> removeBroadcastCallbackById(id.value)
            is RealtimeCallbackId.Presence -> removePresenceCallbackById(id.value)
            is RealtimeCallbackId.Postgres -> removePostgresCallbackById(id.value)
            is RealtimeCallbackId.System -> removeSystemCallbackById(id.value)
            is RealtimeCallbackId.Error -> removeErrorCallbackById(id.value)
        }
    }

    override fun setServerChanges(changes: List<PostgresJoinConfig>) {
        _serverChanges.store(changes)
    }

}