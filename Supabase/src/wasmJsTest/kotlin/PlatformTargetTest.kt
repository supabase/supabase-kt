import io.supabase.CurrentPlatformTarget
import io.supabase.PlatformTarget
import kotlin.test.Test
import kotlin.test.assertEquals

actual class PlatformTargetTest {

    @Test
    actual fun testPlatformTarget() {
        assertEquals(PlatformTarget.WASM_JS, CurrentPlatformTarget, "The current platform target should be WASM_JS")
    }

}