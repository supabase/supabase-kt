import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.jwt.JWTUtils
import io.supabase.auth.jwt.JwtHeader
import io.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.supabase.auth.mfa.FactorType
import io.supabase.auth.mfa.MfaStatus
import io.supabase.auth.minimalConfig
import io.supabase.auth.providers.builtin.Phone
import io.supabase.auth.user.UserInfo
import io.supabase.auth.user.UserMfaFactor
import io.supabase.auth.user.UserSession
import io.supabase.testing.assertMethodIs
import io.supabase.testing.assertPathIs
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.pathAfterVersion
import io.supabase.testing.respondJson
import io.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

class MfaApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testEnrollTOTP() = runTest {
        val friendlyName = "My Factor"
        val issuer = "My App"
        val expectedId = "123"
        val expectedSecret = "secret"
        val expectedQrCode = "qrCode"
        val expectedUri = "uri"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/factors/", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(friendlyName, body["friendly_name"]?.jsonPrimitive?.content)
            assertEquals(FactorType.TOTP.value, body["factor_type"]?.jsonPrimitive?.content)
            assertEquals(issuer, body["issuer"]?.jsonPrimitive?.content)
            respondJson("""
                    {
                    "id": "$expectedId",
                    "totp": {
                        "secret": "$expectedSecret",
                        "qr_code": "$expectedQrCode",
                        "uri": "$expectedUri"
                        }
                    }
                """.trimIndent())
        }
        val factor = client.auth.mfa.enroll(FactorType.TOTP, friendlyName) {
            this.issuer = issuer
        }
        assertEquals(expectedId, factor.id)
        assertEquals(expectedSecret, factor.data.secret)
        assertEquals(expectedQrCode, factor.data.qrCode)
        assertEquals(expectedUri, factor.data.uri)
    }

    @Test
    fun testEnrollPhone() = runTest {
        val friendlyName = "My Factor"
        val expectedId = "123"
        val expectedPhone = "+491638976913"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/factors/", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Post, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(friendlyName, body["friendly_name"]?.jsonPrimitive?.content)
            assertEquals(FactorType.Phone.value, body["factor_type"]?.jsonPrimitive?.content)
            assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
            respondJson("""
                    {
                    "id": "$expectedId",
                    "phone": "$expectedPhone"
                    }
                """.trimIndent())
        }
        val factor = client.auth.mfa.enroll(FactorType.Phone, friendlyName) {
            phone = expectedPhone
        }
        assertEquals(expectedId, factor.id)
        assertEquals(expectedPhone, factor.data.phone)
    }

    @Test
    fun testCreateChallengeWithChannel() {
        testCreateChallenge(true)
    }

    @Test
    fun testCreateChallengeWithoutChannel() {
        testCreateChallenge(false)
    }

    @Test
    fun testVerifyChallengeAndSave() {
        testVerifyChallenge(true)
    }

    @Test
    fun testVerifyChallengeWithoutSave() {
        testVerifyChallenge(false)
    }

    @Test
    fun testUnenrollFactor() = runTest {
        val expectedFactorId = "123"
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/factors/$expectedFactorId", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Delete, it.method)
            respondJson("")
        }
        client.auth.mfa.unenroll(expectedFactorId)
    }

    // Todo: Add AMR entry test
    @Test
    fun testGetAALC1() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL1)
    }

    @Test
    fun testGetAALC2() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2)
    }

    @Test
    fun testGetAALC3() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL2)
    }

    @Test
    fun testGetAALCustom() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL1, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwiYWFsIjoiYWFsMSJ9.8kejyA6926zNuyTWhtDWvTChv7_DoilPe0RCNETQnG4")
    }

    @Test
    fun testRetrieveVerifiedFactors() = runTest {
        val expectedFactor = verifiedFactor()
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            respondJson(UserInfo(id = "id", aud = "aud", factors = listOf(expectedFactor)))
        }
        client.auth.awaitInitialization()
        client.auth.importAuthToken("token")
        val factors = client.auth.mfa.retrieveFactorsForCurrentUser()
        assertEquals(1, factors.size)
        assertEquals(expectedFactor, factors.first())
    }

    @Test
    fun testMfaProperties() = runTest {
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            respond("")
        }
        client.auth.awaitInitialization()
        client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2))
        assertEquals(MfaStatus(enabled = true, active = false), client.auth.mfa.status)
        client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL1))
        assertEquals(MfaStatus(enabled = false, active = true), client.auth.mfa.status)
    }

    @Test
    fun testMfaStatus() = runTest {
        client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            respond("")
        }
        client.auth.awaitInitialization()
        client.auth.mfa.statusFlow.test {
            assertEquals(MfaStatus(enabled = false, active = false), awaitItem())
            client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2))
            assertEquals(MfaStatus(enabled = true, active = false), awaitItem())
            client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL1))
            assertEquals(MfaStatus(enabled = false, active = true), awaitItem())
        }
    }

    private fun createSession(
        currentAAL: AuthenticatorAssuranceLevel,
        nextAAL: AuthenticatorAssuranceLevel
    ): UserSession {
        val data = buildJsonObject {
            put("aal", currentAAL.name.lowercase())
        }
        val header = Json.encodeToString(JwtHeader(JwtHeader.Algorithm.HS256))
        val token = "${JWTUtils.encodeToBase64Url(header)}.${JWTUtils.encodeToBase64Url(data.toString())}.${JWTUtils.encodeToBase64Url("ignore")}"
        val factors = if(nextAAL == AuthenticatorAssuranceLevel.AAL1) emptyList() else listOf(verifiedFactor())
        return userSession(customToken = token, user = UserInfo(id = "id", aud = "aud", factors = factors))
    }

    private fun testGetAAL(
        current: AuthenticatorAssuranceLevel,
        next: AuthenticatorAssuranceLevel,
        customJwt: String? = null
    ) {
        runTest {
            val data = buildJsonObject {
                put("aal", current.name.lowercase())
            }
            val header = Json.encodeToString(JwtHeader(JwtHeader.Algorithm.HS256))
            val token = "${JWTUtils.encodeToBase64Url(header)}.${JWTUtils.encodeToBase64Url(data.toString())}.${JWTUtils.encodeToBase64Url("ignore")}"
            client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                respond("")
            }
            client.auth.awaitInitialization()
            if(customJwt == null) {
                val factors = if(next == AuthenticatorAssuranceLevel.AAL1) emptyList() else listOf(verifiedFactor())
                client.auth.importSession(userSession(customToken = token, user = UserInfo(id = "id", aud = "aud", factors = factors)))
            }
            val (c, n) = client.auth.mfa.getAuthenticatorAssuranceLevel(customJwt)
            assertEquals(current.name.lowercase(), c.name.lowercase())
            assertEquals(next.name.lowercase(), n.name.lowercase())
        }
    }

    private fun testVerifyChallenge(saveSession: Boolean) {
        runTest {
            val expectedFactorId = "123"
            val expectedChallengeId = "456"
            val expectedCode = "789"
            val expectedSession = userSession()
            client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors/$expectedFactorId/verify", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedCode, body["code"]?.jsonPrimitive?.content)
                assertEquals(expectedChallengeId, body["challenge_id"]?.jsonPrimitive?.content)
                respondJson(expectedSession)
            }
            client.auth.awaitInitialization()
            val session = client.auth.mfa.verifyChallenge(expectedFactorId, expectedChallengeId, expectedCode, saveSession)
            assertEquals(expectedSession, session)
            if(saveSession) {
                assertEquals(expectedSession, client.auth.currentSessionOrNull())
            } else {
                assertNull(client.auth.currentSessionOrNull())
            }
        }
    }

    private fun testCreateChallenge(withChannel: Boolean) {
        runTest {
            val expectedFactorId = "123"
            val expectedChannel = "sms"
            val expectedChallengeId = "456"
            val expiresAt = 1L
            client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors/$expectedFactorId/challenge", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                if(withChannel) {
                    assertEquals(expectedChannel, body["channel"]?.jsonPrimitive?.content)
                } else {
                    assertNull(body["channel"])
                }
                respondJson("""
                    {
                    "id": "$expectedChallengeId",
                    "type": "phone",
                    "expires_at": "$expiresAt"
                    }
                """.trimIndent())
            }
            client.auth.awaitInitialization()
            val challenge = client.auth.mfa.createChallenge(expectedFactorId, if(withChannel) Phone.Channel.SMS else null)
            assertEquals(expectedChallengeId, challenge.id)
            assertEquals("phone", challenge.factorType)
            assertEquals(expiresAt, challenge.expiresAt.epochSeconds)
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

    private fun verifiedFactor() = UserMfaFactor("id", Clock.System.now(), Clock.System.now(), "verified", factorType = "")

}