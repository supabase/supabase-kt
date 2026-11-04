import io.supabase.postgrest.query.Columns
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable

class ColumnsTest {

    @Test
    fun testSingleLineColumns() {
        val columns = Columns.raw("id")
        assertEquals("id", columns.value)
        val columns2 = Columns.raw("id,name")
        assertEquals("id,name", columns2.value)
    }

    @Test
    fun testMultiLineColumns() {
        val columns = Columns.raw(
            """
            id,
            name
            """.trimIndent()
        )
        assertEquals("id,name", columns.value)
    }

    @Test
    fun testMultiLineColumnsWithWhitespaces() {
        val columns = Columns.raw(
            """
            id  ,
            name  
            """.trimIndent()
        )
        assertEquals("id,name", columns.value)
    }

    @Test
    fun testMultiLineColumnsWithQuotedWhitespaces() {
        val columns = Columns.raw(
            """
            "id  ",
            name
            """.trimIndent()
        )
        assertEquals("\"id  \",name", columns.value)
    }

    @Serializable
    data class TestModel(val id: Int, val name: String, val age: Int)

    @Test
    fun testTypeColumns() {
        val columns = Columns.type<TestModel>()
        assertEquals("id,name,age", columns.value)
    }

}