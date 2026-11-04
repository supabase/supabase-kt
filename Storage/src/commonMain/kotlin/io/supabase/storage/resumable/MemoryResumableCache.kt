package io.supabase.storage.resumable

import io.supabase.collections.AtomicMutableMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A [ResumableCache] implementation using [AtomicMutableMap]. This implementation saves the urls in memory. If you want a disk based cache, use [Disk].
 * By default, cached urls expire after 30 minutes. You can change this by passing a custom [AtomicMutableMap] to the constructor
 */
class MemoryResumableCache(
    private val map: MutableMap<String, String> = AtomicMutableMap(),
) : ResumableCache {

    override suspend fun set(fingerprint: Fingerprint, entry: ResumableCacheEntry) {
        map[fingerprint.value] = Json.encodeToString(entry)
    }

    override suspend fun get(fingerprint: Fingerprint): ResumableCacheEntry? {
        return map[fingerprint.value]?.let {
            Json.decodeFromString(it)
        }
    }

    override suspend fun remove(fingerprint: Fingerprint) {
        map.remove(fingerprint.value)
    }

    override suspend fun clear() {
        map.clear()
    }

    override suspend fun entries(): List<CachePair> {
        return map.mapNotNull { (key, entry) ->
            Fingerprint(key)?.let { it to Json.decodeFromString<ResumableCacheEntry>(entry) }
        }
    }

}