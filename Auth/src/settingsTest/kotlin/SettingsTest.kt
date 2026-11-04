import io.supabase.auth.createDefaultSettingsKey
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {

    @Test
    fun testSettingsKey() {
        val supabaseUrl = "id.supabase.co"
        val key = createDefaultSettingsKey(supabaseUrl)
        assertEquals("sb-id-supabase-co", key)
    }

}