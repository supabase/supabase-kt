package io.supabase.integration

import io.supabase.auth.auth
import io.supabase.postgrest.from
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostgrestIntegrationTest : IntegrationTestBase() {

    @Serializable
    data class TestItem(
        val id: String? = null,
        val name: String,
        val description: String? = null,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Test
    fun testInsertAndSelect() = runTest {
        val client = createAuthenticatedClient()
        val userId = client.auth.currentUserOrNull()?.id

        val itemName = "test-item-${System.nanoTime()}"
        client.from("test_items").insert(TestItem(name = itemName, description = "A test item", userId = userId))

        val items = client.from("test_items").select {
            filter {
                eq("name", itemName)
            }
        }.decodeList<TestItem>()

        assertEquals(1, items.size)
        assertEquals(itemName, items.first().name)
        assertEquals("A test item", items.first().description)
    }

    @Test
    fun testUpdate() = runTest {
        val client = createAuthenticatedClient()
        val userId = client.auth.currentUserOrNull()?.id

        val itemName = "update-test-${System.nanoTime()}"
        client.from("test_items").insert(TestItem(name = itemName, description = "original", userId = userId))

        client.from("test_items").update({
            set("description", "updated")
        }) {
            filter {
                eq("name", itemName)
            }
        }

        val items = client.from("test_items").select {
            filter {
                eq("name", itemName)
            }
        }.decodeList<TestItem>()

        assertEquals("updated", items.first().description)
    }

    @Test
    fun testDelete() = runTest {
        val client = createAuthenticatedClient()
        val userId = client.auth.currentUserOrNull()?.id

        val itemName = "delete-test-${System.nanoTime()}"
        client.from("test_items").insert(TestItem(name = itemName, description = "to delete", userId = userId))

        client.from("test_items").delete {
            filter {
                eq("name", itemName)
            }
        }

        val items = client.from("test_items").select {
            filter {
                eq("name", itemName)
            }
        }.decodeList<TestItem>()

        assertTrue(items.isEmpty())
    }

    @Test
    fun testRlsEnforcement() = runTest {
        val clientA = createAuthenticatedClient()
        val userIdA = clientA.auth.currentUserOrNull()?.id

        val itemName = "rls-test-${System.nanoTime()}"
        clientA.from("test_items").insert(TestItem(name = itemName, description = "user A's item", userId = userIdA))

        // User B should not see User A's items
        val clientB = createAuthenticatedClient()
        val items = clientB.from("test_items").select {
            filter {
                eq("name", itemName)
            }
        }.decodeList<TestItem>()

        assertTrue(items.isEmpty(), "User B should not see User A's items due to RLS")
    }

    @Test
    fun testSelectWithFilters() = runTest {
        val client = createAuthenticatedClient()
        val userId = client.auth.currentUserOrNull()?.id
        val prefix = "filter-test-${System.nanoTime()}"

        // Insert multiple items
        client.from("test_items").insert(listOf(
            TestItem(name = "${prefix}-alpha", description = "first", userId = userId),
            TestItem(name = "${prefix}-beta", description = "second", userId = userId),
            TestItem(name = "${prefix}-gamma", description = "third", userId = userId)
        ))

        // Test eq filter
        val eqResult = client.from("test_items").select {
            filter {
                eq("name", "${prefix}-alpha")
            }
        }.decodeList<TestItem>()
        assertEquals(1, eqResult.size)

        // Test like filter
        val likeResult = client.from("test_items").select {
            filter {
                like("name", "${prefix}%")
            }
        }.decodeList<TestItem>()
        assertEquals(3, likeResult.size)
    }

    @Test
    fun testAnonRead() = runTest {
        // First create an item as authenticated user
        val authClient = createAuthenticatedClient()
        val userId = authClient.auth.currentUserOrNull()?.id
        val itemName = "anon-test-${System.nanoTime()}"
        authClient.from("test_items").insert(TestItem(name = itemName, description = "visible to anon", userId = userId))

        // Read as anon
        val anonClient = createTestClient()
        val items = anonClient.from("test_items").select {
            filter {
                eq("name", itemName)
            }
        }.decodeList<TestItem>()

        assertEquals(1, items.size)
        assertEquals(itemName, items.first().name)
    }
}
