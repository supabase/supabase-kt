import io.supabase.dsl.required
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestRequired {

    class Dummy {

        var property: String by required()

    }

    @Test
    fun testRequired() {
        val dummy = Dummy()
        assertFailsWith<IllegalStateException> {
            dummy.property
        }
        dummy.property = "value"
        assertEquals(dummy.property, "value")
    }

}