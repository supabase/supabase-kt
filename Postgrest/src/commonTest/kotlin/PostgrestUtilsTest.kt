package io.supabase.postgrest

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgrestUtilsTest {

    @Serializable
    data class TestModel(val id: Int, val name: String, val age: Int)

    class NonSerializableModel(val id: Int)

    @Test
    fun testClassPropertyNamesWithSerializableClass() {
        val properties = classPropertyNames<TestModel>()
        assertEquals(listOf("id", "name", "age"), properties)
    }

    @Test
    fun testClassPropertyNamesWithNonSerializableClass() {
        val exception = assertFailsWith<IllegalArgumentException> {
            classPropertyNames<NonSerializableModel>()
        }
        assertEquals("Could not find serializer for NonSerializableModel", exception.message)
    }

}
