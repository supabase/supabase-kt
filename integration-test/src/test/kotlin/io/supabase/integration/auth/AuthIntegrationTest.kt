package io.supabase.integration.auth

import io.supabase.auth.auth
import io.supabase.auth.providers.builtin.Email
import io.supabase.auth.status.SessionStatus
import io.supabase.exceptions.RestException
import io.supabase.integration.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AuthIntegrationTest : IntegrationTestBase() {

    @Test
    fun testSignUpWithEmail() = runTest {
        val client = createTestClient()
        val email = "signup-${System.nanoTime()}@example.com"
        val password = "test-password-123!"

        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        val status = client.auth.sessionStatus.value
        assertIs<SessionStatus.Authenticated>(status)
        assertNotNull(status.session.accessToken)
    }

    @Test
    fun testSignInWithEmail() = runTest {
        val client = createTestClient()
        val email = "signin-${System.nanoTime()}@example.com"
        val password = "test-password-123!"

        // Sign up first
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        client.auth.signOut()

        // Sign in
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        val status = client.auth.sessionStatus.value
        assertIs<SessionStatus.Authenticated>(status)
        assertNotNull(status.session.accessToken)
        assertNotNull(status.session.refreshToken)
    }

    @Test
    fun testSignOut() = runTest {
        val client = createAuthenticatedClient()

        assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)

        client.auth.signOut()

        assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
    }

    @Test
    fun testGetUser() = runTest {
        val client = createAuthenticatedClient()

        val user = client.auth.retrieveUserForCurrentSession()

        assertNotNull(user.id)
        assertNotNull(user.email)
    }

    @Test
    fun testUpdateUser() = runTest {
        val client = createAuthenticatedClient()

        val updatedUser = client.auth.updateUser {
            data {
                put("display_name", JsonPrimitive("Test User"))
            }
        }

        assertNotNull(updatedUser.userMetadata)
        assertEquals(
            "Test User",
            updatedUser.userMetadata?.get("display_name")?.let {
                (it as? JsonPrimitive)?.content
            }
        )
    }

    @Test
    fun testSignUpDuplicateEmail() = runTest {
        val client = createTestClient()
        val email = "duplicate-${System.nanoTime()}@example.com"
        val password = "test-password-123!"

        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        // Create a second client to attempt duplicate signup
        val client2 = createTestClient()
        assertFailsWith<RestException> {
            client2.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }
}