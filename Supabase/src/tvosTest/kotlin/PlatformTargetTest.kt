import io.supabase.CurrentPlatformTarget
import io.supabase.PlatformTarget
import kotlin.test.Test
import kotlin.test.assertEquals

actual class PlatformTargetTest {

    @Test
    actual fun testPlatformTarget() {
        assertEquals(PlatformTarget.TVOS, CurrentPlatformTarget, "The current platform target should be TVOS")
    }

}