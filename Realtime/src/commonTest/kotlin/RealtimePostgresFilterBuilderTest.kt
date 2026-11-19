import io.supabase.realtime.postgres.RealtimePostgresChangesFilterOperator
import io.supabase.realtime.postgres.RealtimePostgresFilterBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimePostgresFilterBuilderTest {

    @Test
    fun buildSingleEqFilter() {
        assertEquals("id=eq.1", buildFilter { eq("id", 1) })
    }

    @Test
    fun buildMultipleFilters() {
        assertEquals("id=eq.1,name=like.foo", buildFilter {
            eq("id", 1)
            like("name", "foo")
        })
    }

    @Test
    fun buildEscapesReservedCharacters() {
        assertEquals("name=eq.\"foo,bar\"", buildFilter {
            eq("name", "foo,bar")
        })
    }

    @Test
    fun buildInAndNotFilters() {
        assertEquals("id=in.(1,2,3),other=not.eq.4,distinct=isdistinct.5", buildFilter {
            isIn("id", listOf(1, 2, 3))
            not("other", RealtimePostgresChangesFilterOperator.EQ, 4)
            isDistinct("distinct", 5)
        })
    }

    private fun buildFilter(block: RealtimePostgresFilterBuilder.() -> Unit): String {
        return RealtimePostgresFilterBuilder().apply(block).build()
    }
}