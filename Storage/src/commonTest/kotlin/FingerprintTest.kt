import io.supabase.storage.resumable.Fingerprint
import io.supabase.storage.resumable.MemoryResumableCache
import io.supabase.storage.resumable.ResumableCacheEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

class FingerprintTest {

    private val size = 1234L

    @Test
    fun testSourceAndSizeOfAPlainSource() {
        val fingerprint = Fingerprint("/home/user/file.txt", size)
        assertEquals("/home/user/file.txt", fingerprint.source)
        assertEquals(size, fingerprint.size)
    }

    @Test
    fun testSourceAndSizeOfASourceContainingTheSeparator() {
        val source = "/home/user/std::vector.txt"
        val fingerprint = Fingerprint(source, size)
        assertEquals(source, fingerprint.source)
        assertEquals(size, fingerprint.size)
    }

    @Test
    fun testParsingAValueWhoseSourceContainsTheSeparator() {
        val source = "/home/user/std::vector.txt"
        val value = Fingerprint(source, size).value
        val parsed = assertNotNull(Fingerprint(value))
        assertEquals(source, parsed.source)
        assertEquals(size, parsed.size)
    }

    @Test
    fun testParsingAValueWithoutASeparator() {
        assertNull(Fingerprint("/home/user/file.txt"))
    }

    @Test
    fun testParsingAValueWithoutANumericSize() {
        assertNull(Fingerprint("/home/user/file.txt::size"))
    }

    @Test
    fun testCacheEntriesOfASourceContainingTheSeparator() {
        runTest {
            val cache = MemoryResumableCache()
            val fingerprint = Fingerprint("/home/user/std::vector.txt", size)
            val entry = ResumableCacheEntry(
                url = "https://projectref.supabase.co/upload",
                path = "path",
                bucketId = "bucketId",
                expiresAt = Clock.System.now(),
                upsert = false,
                contentType = "text/plain"
            )
            cache.set(fingerprint, entry)
            assertEquals(listOf(fingerprint to entry), cache.entries())
        }
    }

}
