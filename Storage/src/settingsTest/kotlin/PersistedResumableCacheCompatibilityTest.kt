import com.russhwolf.settings.MapSettings
import io.supabase.storage.resumable.Fingerprint
import io.supabase.storage.resumable.SettingsResumableCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

/**
 * Checks that resumable upload state stored on user devices by an older SDK version can still be loaded,
 * so uploads can be resumed after updating.
 *
 * The fixture was produced by the published `io.github.jan-tennert.supabase:storage-kt:3.9.0` artifact and must never
 * be regenerated with a newer version.
 */
class PersistedResumableCacheCompatibilityTest {

    @Test
    fun testResumableEntryPersistedBy390IsRestored() = runTest {
        val cache = SettingsResumableCache(MapSettings(FINGERPRINT to ENTRY_390))
        val entry = assertNotNull(cache.get(assertNotNull(Fingerprint(FINGERPRINT))))
        assertEquals("https://abcdefghijklmnop.supabase.co/storage/v1/upload/resumable/abc123", entry.url)
        assertEquals(Instant.parse("2026-11-05T12:00:00Z"), entry.expiresAt)
        assertEquals(Fingerprint("avatars/folder/video.mp4", 52_428_800L), cache.entries().single().first)
    }

    private companion object {
        const val FINGERPRINT = "avatars/folder/video.mp4::52428800"
        const val ENTRY_390 = """{"url":"https://abcdefghijklmnop.supabase.co/storage/v1/upload/resumable/abc123","path":"folder/video.mp4","bucketId":"avatars","expiresAt":"2026-11-05T12:00:00Z","upsert":true,"contentType":"video/mp4"}"""
    }

}
