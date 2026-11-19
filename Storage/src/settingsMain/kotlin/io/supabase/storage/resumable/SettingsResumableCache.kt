package io.supabase.storage.resumable

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import io.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_NODE
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A [ResumableCache] implementation using [com.russhwolf.settings.Settings]. This implementation saves the urls on the disk. If you want a memory only cache, use [Memory].
 * Unsupported on Linux.
 */
@OptIn(ExperimentalSettingsApi::class)
class SettingsResumableCache(settings: Settings = Settings()) : ResumableCache {

    private val settings = settings.toSuspendSettings()

    override suspend fun set(fingerprint: Fingerprint, entry: ResumableCacheEntry) {
        settings.putString(fingerprint.value, Json.encodeToString(entry))
    }

    override suspend fun get(fingerprint: Fingerprint): ResumableCacheEntry? {
        return settings.getStringOrNull(fingerprint.value)?.let {
            Json.decodeFromString(it)
        }
    }

    override suspend fun remove(fingerprint: Fingerprint) {
        settings.remove(fingerprint.value)
    }

    override suspend fun clear() {
        settings.keys().forEach { key ->
            Fingerprint(key)?.let { remove(it) }
        }
    }

    override suspend fun entries(): List<CachePair> {
        return settings.keys().mapNotNull { key ->
            Fingerprint(key) //filter out invalid fingerprints
        }.map {
            it to (get(it) ?: error("No entry found for $it"))
        }
    }

}

@SupabaseInternal
actual fun createDefaultResumableCache(): ResumableCache = if(!IS_NODE) SettingsResumableCache() else MemoryResumableCache()