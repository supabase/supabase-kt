import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.auth.user.UserSession
import io.supabase.testing.assertMethodIs
import io.supabase.testing.assertPathIs
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.pathAfterVersion
import io.supabase.testing.respondJson
import io.supabase.testing.toJsonElement
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthPasskeyApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testStartRegistration() = runTest {
        val expectedChallengeId = "challenge-123"
        val expectedExpiresAt = "2024-12-31T23:59:59Z"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/registration/options", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            respondJson("""
                {
                    "challenge_id": "$expectedChallengeId",
                    "expires_at": "$expectedExpiresAt",
                    "options": {
                        "challenge": "dGVzdGNoYWxsZW5nZQ==",
                        "timeout": 60000,
                        "attestation": "direct",
                        "user": {
                            "id": "dXNlcmlk",
                            "name": "user@example.com",
                            "displayName": "User Name"
                        },
                        "pubKeyCredParams": [
                            {
                                "alg": -7,
                                "type": "public-key"
                            }
                        ],
                        "rp": {
                            "name": "Example",
                            "id": "example.com"
                        }
                    }
                }
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val response = client.auth.passkeys.startRegistration()
        assertEquals(expectedChallengeId, response.challengeId)
        assertEquals(expectedExpiresAt, response.expiresAt.toString())
    }

    @Test
    fun testVerifyRegistration() = runTest {
        val challengeId = "challenge-123"
        val credential = """{"id":"test","type":"public-key","response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiZEdWemRBPT0iLCJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tIn0=","attestationObject":"o2NmbXRmcGFja2VkZ2F0dFN0bXSjY2FsZyZjc2lnWEcwRQIhAJZ7VN4v..."}}"""
        val expectedId = "passkey-123"
        val expectedFriendlyName = "My Passkey"
        val expectedCreatedAt = "2024-01-01T00:00:00Z"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/registration/verify", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(challengeId, body["challenge_id"]?.jsonPrimitive?.content)
            respondJson("""
                {
                    "id": "$expectedId",
                    "friendly_name": "$expectedFriendlyName",
                    "created_at": "$expectedCreatedAt"
                }
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val response = client.auth.passkeys.verifyRegistration(challengeId, credential)
        assertEquals(expectedId, response.id)
        assertEquals(expectedFriendlyName, response.friendlyName)
    }

    @Test
    fun testStartAuthenticationWithoutCaptcha() = runTest {
        val expectedChallengeId = "challenge-456"
        val expectedExpiresAt = 1735689600L
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/authentication/options", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertNull(body["gotrue_meta_security"])
            respondJson("""
                {
                    "challenge_id": "$expectedChallengeId",
                    "expires_at": $expectedExpiresAt,
                    "options": {
                        "challenge": "dGVzdGNoYWxsZW5nZQ==",
                        "timeout": 60000,
                        "userVerification": "preferred",
                        "rpId": "example.com",
                        "allowCredentials": []
                    }
                }
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val response = client.auth.passkeys.startAuthentication()
        assertEquals(expectedChallengeId, response.challengeId)
        assertEquals(expectedExpiresAt, response.expiresAt.epochSeconds)
    }

    @Test
    fun testStartAuthenticationWithCaptcha() = runTest {
        val expectedChallengeId = "challenge-456"
        val expectedExpiresAt = 1735689600
        val captchaToken = "captcha-token-xyz"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/authentication/options", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(captchaToken, body["gotrue_meta_security"]?.jsonObject?.get("captcha_token")?.jsonPrimitive?.content)
            respondJson("""
                {
                    "challenge_id": "$expectedChallengeId",
                    "expires_at": $expectedExpiresAt,
                    "options": {
                        "challenge": "dGVzdGNoYWxsZW5nZQ==",
                        "timeout": 60000,
                        "userVerification": "preferred",
                        "rpId": "example.com",
                        "allowCredentials": []
                    }
                }
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val response = client.auth.passkeys.startAuthentication {
            this.captchaToken = captchaToken
        }
        assertEquals(expectedChallengeId, response.challengeId)
    }

    @Test
    fun testVerifyAuthentication() = runTest {
        val challengeId = "challenge-456"
        val credential = """{"id":"test","type":"public-key","response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiZEdWemRBPT0iLCJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tIn0=","authenticatorData":"SZYN5OtPZAElAwgsB0AxvUSBY2BhWaOmFfHqMx7XsHcFAAAAAA==","signature":"MEQCIB..."}}"""
        val expectedSession = userSession()
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/authentication/verify", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(challengeId, body["challenge_id"]?.jsonPrimitive?.content)
            // server now returns the session directly
            respondJson(Json.encodeToString(UserSession.serializer(), expectedSession))
        }
        client.auth.awaitInitialization()
        val response = client.auth.passkeys.verifyAuthentication(challengeId, credential)
        // verify returned session and that it was saved into the auth plugin
        assertEquals(expectedSession, response)
        assertEquals(expectedSession, client.auth.currentSessionOrNull())
    }

    @Test
    fun testList() = runTest {
        val passkeyId1 = "passkey-1"
        val passkeyId2 = "passkey-2"
        val friendlyName1 = "Work Passkey"
        val friendlyName2 = "Mobile Passkey"
        val createdAt = "2024-01-01T00:00:00Z"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Get, it.method)
            respondJson("""
                [
                    {
                        "id": "$passkeyId1",
                        "friendly_name": "$friendlyName1",
                        "created_at": "$createdAt",
                        "last_used_at": null
                    },
                    {
                        "id": "$passkeyId2",
                        "friendly_name": "$friendlyName2",
                        "created_at": "$createdAt",
                        "last_used_at": "$createdAt"
                    }
                ]
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val passkeys = client.auth.passkeys.list()
        assertEquals(2, passkeys.size)
        assertEquals(passkeyId1, passkeys[0].id)
        assertEquals(friendlyName1, passkeys[0].friendlyName)
        assertEquals(passkeyId2, passkeys[1].id)
        assertEquals(friendlyName2, passkeys[1].friendlyName)
    }

    @Test
    fun testListEmpty() = runTest {
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Get, it.method)
            respondJson("[]")
        }
        client.auth.awaitInitialization()
        val passkeys = client.auth.passkeys.list()
        assertEquals(0, passkeys.size)
    }

    @Test
    fun testDelete() = runTest {
        val passkeyId = "passkey-123"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/$passkeyId", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Delete, it.method)
            respondJson("")
        }
        client.auth.awaitInitialization()
        client.auth.passkeys.delete(passkeyId)
    }

    @Test
    fun testUpdate() = runTest {
        val passkeyId = "passkey-123"
        val newFriendlyName = "Updated Passkey Name"
        val createdAt = "2024-01-01T00:00:00Z"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/$passkeyId", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Patch, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(newFriendlyName, body["friendly_name"]?.jsonPrimitive?.content)
            respondJson("""
                {
                    "id": "$passkeyId",
                    "friendly_name": "$newFriendlyName",
                    "created_at": "$createdAt",
                    "last_used_at": null
                }
            """.trimIndent())
        }
        client.auth.awaitInitialization()
        val updatedPasskey = client.auth.passkeys.update(passkeyId, newFriendlyName)
        assertEquals(passkeyId, updatedPasskey.id)
        assertEquals(newFriendlyName, updatedPasskey.friendlyName)
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if (::client.isInitialized) {
                client.close()
            }
        }
    }

}
