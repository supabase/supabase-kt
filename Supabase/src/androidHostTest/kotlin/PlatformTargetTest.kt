import io.supabase.CurrentPlatformTarget
import io.supabase.PlatformTarget
import org.junit.Test
import kotlin.test.assertEquals

actual class PlatformTargetTest {

    @Test
    actual fun testPlatformTarget() {
        assertEquals(PlatformTarget.ANDROID, CurrentPlatformTarget, "The current platform target should be ANDROID")
    }

}