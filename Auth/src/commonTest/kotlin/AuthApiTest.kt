import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.OtpVerifyResult
import io.github.jan.supabase.auth.PKCEConstants
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.InvalidJwtException
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class AuthRequestTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
            flowType = FlowType.PKCE
        }
    }

    private lateinit var client: SupabaseClient
    
    @Test
    fun testSignUpWithEmailNoAutoConfirm() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }.awaitInit()
            val user = client.auth.signUpWith(Email, redirectUrl = expectedUrl) {
                email = expectedEmail
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertEquals(expectedEmail, user?.email, "Email should be equal")
        }
    }

    @Test
    fun testSignUpWithEmailAutoConfirm() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleSessionWithUserData(email = "example@email.com", phone = "+1234567890")
                )
            }.awaitInit()
            val user = client.auth.signUpWith(Email) {
                email = expectedEmail
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertNotNull(user)
            assertEquals(expectedEmail, user.email, "Email should be equal")
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignUp(Email))
        }
    }

    @Test
    fun testSignUpWithEmailAutoConfirmWithoutUserData() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            val user = client.auth.signUpWith(Email) {
                email = expectedEmail
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertNull(user)
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignUp(Email))
        }
    }

    @Test
    fun testSignUpWithPhoneAutoConfirm() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleSessionWithUserData()
                )
            }.awaitInit()
            val user = client.auth.signUpWith(Phone) {
                phone = expectedPhone
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertNotNull(user)
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignUp(Phone))
        }
    }

    @Test
    fun testSignUpWithPhoneNoAutoConfirm() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(phone = expectedPhone)
                )
            }.awaitInit()
            val user = client.auth.signUpWith(Phone, redirectUrl = expectedUrl) {
                phone = expectedPhone
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertEquals(expectedPhone, user?.phone, "Phone should be equal")
        }
    }

    @Test
    fun testSignUpOtpWithPhone() {
        runTest {
            val expectedPhone = "+1234567890"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respond("")
            }.awaitInit()
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                phone = expectedPhone
                this.captchaToken = captchaToken
                data = userData
            }
        }
    }

    @Test
    fun testSignUpOtpWithEmail() {
        runTest {
            val expectedEmail = "example@email.com"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respond("")
            }.awaitInit()
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                email = expectedEmail
                this.captchaToken = captchaToken
                data = userData
            }
        }
    }

    @Test
    fun testSignUpOtpWithPhoneWhatsApp() {
        runTest {
            val expectedPhone = "+1234567890"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals("whatsapp", body["channel"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respond("")
            }.awaitInit()
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                phone = expectedPhone
                channel = Phone.Channel.WHATSAPP
                this.captchaToken = captchaToken
                data = userData
            }
        }
    }

    @Test
    fun testSignUpOtpWithPhoneSmsChannel() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals("sms", body["channel"]?.jsonPrimitive?.content)
                containsCodeChallenge(body)
                respond("")
            }.awaitInit()
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                phone = expectedPhone
                channel = Phone.Channel.SMS
            }
        }
    }

    @Test
    fun testSignUpOtpWithEmailIgnoresChannel() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertNull(body["channel"], "Channel should not be present for email OTP")
                containsCodeChallenge(body)
                respond("")
            }.awaitInit()
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                email = expectedEmail
                channel = Phone.Channel.WHATSAPP // This should be ignored for email
            }
        }
    }

    @Test
    fun testSignInWithIDToken() {
        runTest {
            val captchaToken = "captchaToken"
            val expectedIdToken = "idToken"
            val expectedProvider = Google
            val expectedAccessToken = "accessToken"
            val expectedNonce = "nonce"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                assertEquals("id_token", params["grant_type"])
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(expectedIdToken, body["id_token"]?.jsonPrimitive?.content)
                assertEquals(expectedProvider.name, body["provider"]?.jsonPrimitive?.content)
                assertEquals(expectedAccessToken, body["access_token"]?.jsonPrimitive?.content)
                assertEquals(expectedNonce, body["nonce"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            client.auth.signInWith(IDToken) {
                this.captchaToken = captchaToken
                this.idToken = expectedIdToken
                provider = expectedProvider
                this.nonce = expectedNonce
                accessToken = expectedAccessToken
            }
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignIn(IDToken))
        }
    }

    @Test
    fun testSignInAnonymously() {
        runTest {
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            client.auth.signInAnonymously(
                captchaToken = captchaToken,
                data = userData
            )
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.AnonymousSignIn)
        }
    }

    @Test
    fun testLinkIdentity() {
        runTest {
            val expectedProvider = Google
            val expectedRedirectUrl = "https://example.com"
            val expectedScopes = listOf("scope1", "scope2")
            val expectedUrlParams = mapOf("key" to "value")
            val providerUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                val params = it.url.parameters
                assertEquals(expectedRedirectUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/user/identities/authorize", it.url.pathAfterVersion())
                assertEquals(expectedProvider.name, params["provider"])
                assertNotNull(params["code_challenge"])
                assertEquals(PKCEConstants.CHALLENGE_METHOD, params["code_challenge_method"])
                assertEquals(expectedScopes.joinToString(" "), params["scopes"])
                assertEquals("value", params["key"])
                respondJson(
                    """
                    {
                        "url": "$providerUrl"
                    }
                    """.trimIndent()
                )
            }.awaitInit()
            val url = client.auth.linkIdentity(expectedProvider, redirectUrl = expectedRedirectUrl) {
                scopes.addAll(expectedScopes)
                queryParams.putAll(expectedUrlParams)
                automaticallyOpenUrl = false
            }
            assertEquals(providerUrl, url)
        }
    }

    @Test
    fun testLinkIdentityWithIdToken() {
        runTest {
            val expectedProvider = Google
            val expectedIdToken = "idToken"
            val expectedAccessToken = "accessToken"
            val expectedNonce = "nonce"
            client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val params = it.url.parameters
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                assertEquals("id_token", params["grant_type"])
                assertEquals(expectedIdToken, body["id_token"]?.jsonPrimitive?.content)
                assertEquals(expectedProvider.name, body["provider"]?.jsonPrimitive?.content)
                assertEquals(expectedAccessToken, body["access_token"]?.jsonPrimitive?.content)
                assertEquals(expectedNonce, body["nonce"]?.jsonPrimitive?.content)
                // ensure we signal linking
                assertEquals("true", body["link_identity"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            client.auth.linkIdentityWithIdToken(expectedProvider, expectedIdToken) {
                accessToken = expectedAccessToken
                nonce = expectedNonce
            }
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
        }
    }

    @Test
    fun testUnlinkIdentity() {
        runTest {
            val expectedIdentityId = "identityId"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs("/user/identities/$expectedIdentityId", it.url.pathAfterVersion())
                respondJson(
                    sampleUserObject()
                )
            }.awaitInit()
            client.auth.unlinkIdentity(expectedIdentityId)
        }
    }

    @Test
    fun testRetrieveSSOUrlWithDomain() {
        runTest {
            val expectedRedirectUrl = "https://example.com"
            val expectedDomain = "https://example.com"
            val expectedCaptchaToken = "captchaToken"
            val expectedUrl = "https://ssourl.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/sso", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRedirectUrl, body["redirect_to"]!!.jsonPrimitive.content)
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedDomain, body["domain"]?.jsonPrimitive?.content)
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                respondJson(
                    """
                    {
                        "url": "$expectedUrl"
                    }
                    """.trimIndent()
                )
            }.awaitInit()
            val result = client.auth.retrieveSSOUrl(redirectUrl = expectedRedirectUrl) {
                this.domain = expectedDomain
                this.captchaToken = expectedCaptchaToken
            }
            assertEquals(expectedUrl, result.url)
        }
    }

    @Test
    fun testRetrieveSSOUrlWithProviderId() {
        runTest {
            val expectedRedirectUrl = "https://example.com"
            val expectedProviderId = "providerId"
            val expectedCaptchaToken = "captchaToken"
            val expectedUrl = "https://ssourl.com"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/sso", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRedirectUrl, body["redirect_to"]!!.jsonPrimitive.content)
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedProviderId, body["provider_id"]?.jsonPrimitive?.content)
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                respondJson(
                    """
                    {
                        "url": "$expectedUrl"
                    }
                    """.trimIndent()
                )
            }.awaitInit()
            val result = client.auth.retrieveSSOUrl(redirectUrl = expectedRedirectUrl) {
                this.providerId = expectedProviderId
                this.captchaToken = expectedCaptchaToken
            }
            assertEquals(expectedUrl, result.url)
        }
    }

    @Test
    fun testUpdateUser() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPhone = "+1234567890"
            val expectedData = buildJsonObject {
                put("key", "value")
            }
            val expectedPassword = "password"
            val expectedCurrentPassword = "current"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Put, it.method)
                assertPathIs("/user", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedData, body["data"]!!.jsonObject)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(expectedCurrentPassword, body["currentPassword"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserObject(email = expectedEmail, phone = expectedPhone)
                )
            }.awaitInit()
            val user = client.auth.updateUser {
                email = expectedEmail
                phone = expectedPhone
                data = expectedData
                password = expectedPassword
                currentPassword = expectedCurrentPassword
            }
            assertEquals(expectedEmail, user.email, "Email should be equal")
            assertEquals(expectedPhone, user.phone, "Phone should be equal")
        }
    }

    @Test
    fun testResendEmailPKCE() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedType = OtpType.Email.SIGNUP
            val expectedCaptchaToken = "captchaToken"
            val expectedUrl = "https://example.com"
            client = createMockedSupabaseClient(configuration = {
                configuration()
            }) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/resend", it.url.pathAfterVersion())
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])

                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                assertNotNull(body["code_challenge"])
                assertNotNull(body["code_challenge_method"])
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }.awaitInit()
            client.auth.resendEmail(expectedType, expectedEmail, expectedCaptchaToken, expectedUrl)
        }
    }

    @Test
    fun testResendPhone() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedType = OtpType.Phone.PHONE_CHANGE
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/resend", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                assertNull(body["code_challenge"])
                respondJson(
                    sampleUserObject(email = expectedPhone)
                )
            }.awaitInit()
            client.auth.resendPhone(expectedType, expectedPhone, expectedCaptchaToken)
        }
    }

    @Test
    fun testResetPasswordForEmail() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedCaptchaToken = "captchaToken"
            val expectedRedirectUrl = "https://example.com?someParama=true&another=one" // Test that url params aren't stripped away
            val encodedRedirectUrl = "https%3A%2F%2Fexample.com%3FsomeParama%3Dtrue%26another%3Done"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/recover", it.url.pathAfterVersion())
                val params = it.url.parameters
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertContains(it.url.toString(), encodedRedirectUrl)
                assertEquals(expectedRedirectUrl, params["redirect_to"])
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }.awaitInit()
            client.auth.resetPasswordForEmail(expectedEmail, expectedRedirectUrl, expectedCaptchaToken)
        }
    }

    @Test
    fun testReauthenticate() {
        runTest {
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/reauthenticate", it.url.pathAfterVersion())
                respond("")
            }.awaitInit()
            client.auth.reauthenticate()
        }
    }

    @Test
    fun testVerifyEmailOtp() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedToken = "token"
            val expectedEmail = "example@email.com"
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedToken, body["token"]?.jsonPrimitive?.content)
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            assertIs<OtpVerifyResult.Authenticated>(client.auth.verifyEmailOtp(expectedType, expectedEmail, expectedToken, expectedCaptchaToken))
        }
    }

    @Test
    fun testVerifyEmailOtpNoSession() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedToken = "token"
            val expectedEmail = "example@email.com"
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedToken, body["token"]?.jsonPrimitive?.content)
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    buildJsonObject {
                        put("status", "ok") // verified but no session
                    }
                )
            }.awaitInit()
            assertIs<OtpVerifyResult.VerifiedNoSession>(client.auth.verifyEmailOtp(expectedType, expectedEmail, expectedToken, expectedCaptchaToken))
        }
    }

    @Test
    fun testVerifyEmailOtpWithTokenHash() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedTokenHash = "hash"
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedTokenHash, body["token_hash"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            val result = client.auth.verifyEmailOtp(expectedType, tokenHash = expectedTokenHash, captchaToken = expectedCaptchaToken)
            assertIs<OtpVerifyResult.Authenticated>(result)
        }
    }

    @Test
    fun testVerifyEmailOtpWithTokenHashNoSession() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedTokenHash = "hash"
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedTokenHash, body["token_hash"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    buildJsonObject {
                        put("status", "ok") // verified but no session
                    }
                )
            }.awaitInit()
            assertIs<OtpVerifyResult.VerifiedNoSession>(client.auth.verifyEmailOtp(expectedType, tokenHash = expectedTokenHash, captchaToken = expectedCaptchaToken))
        }
    }

    @Test
    fun testVerifyPhoneOtp() {
        runTest {
            val expectedType = OtpType.Phone.SMS
            val expectedToken = "token"
            val expectedPhone = "+1234567890"
            val expectedCaptchaToken = "captchaToken"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedToken, body["token"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }.awaitInit()
            client.auth.verifyPhoneOtp(expectedType, expectedPhone, expectedToken, expectedCaptchaToken)
        }
    }

    @Test
    fun testRetrieveUser() {
        runTest {
            val expectedJWT = "token"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/user", it.url.pathAfterVersion())
                assertEquals("Bearer $expectedJWT", it.headers["Authorization"])
                respondJson(
                    sampleUserObject()
                )
            }.awaitInit()
            val user = client.auth.retrieveUser(expectedJWT)
            assertNotNull(user, "User should not be null")
        }
    }

    @Test
    fun testSignOut() {
        runTest {
            val expectedScope = SignOutScope.LOCAL
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/logout", it.url.pathAfterVersion())
                val parameters = it.url.parameters
                assertEquals(expectedScope.name.lowercase(), parameters["scope"])
                respond("")
            }.awaitInit()
            client.auth.awaitInitialization()
            client.auth.importSession(Json.decodeFromString(sampleUserSession()))
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            client.auth.signOut(expectedScope)
            assertNull(client.auth.currentSessionOrNull(), "Session should be null")
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
        }
    }

    @Test
    fun testRefreshSession() {
        runTest {
            val configurationWithExpiredSession: SupabaseClientBuilder.() -> Unit = {
                install(Auth) {
                    minimalConfig()
                    autoLoadFromStorage = true
                    sessionManager = MemorySessionManager(userSession(expiresIn = 0).copy(expiresAt = Clock.System.now()-5.minutes))
                    flowType = FlowType.PKCE
                }
            }
            val expectedRefreshToken = "refreshToken"
            val expectedSession = userSession()
            client = createMockedSupabaseClient(configuration = configurationWithExpiredSession) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                val parameters = it.url.parameters
                assertEquals("refresh_token", parameters["grant_type"])
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRefreshToken, body["refresh_token"]?.jsonPrimitive?.content)
                respondJson(
                    expectedSession
                )
            }.awaitInit()
            client.auth.awaitInitialization()
            client.auth.config.alwaysAutoRefresh = true // this config override is for catching edge cases (like for #1132)
            val session = client.auth.refreshSession(expectedRefreshToken)
            assertEquals(expectedSession, session)
        }
    }

    @Test
    fun testRefreshCurrentSession() {
        runTest {
            val configurationWithExpiredSession: SupabaseClientBuilder.() -> Unit = {
                install(Auth) {
                    minimalConfig()
                    autoLoadFromStorage = true
                    sessionManager = MemorySessionManager(userSession(expiresIn = 0).copy(expiresAt = Clock.System.now()-5.minutes, refreshToken = "refreshToken"))
                    flowType = FlowType.PKCE
                }
            }
            val expectedRefreshToken = "refreshToken"
            val expectedSession = userSession()
            client = createMockedSupabaseClient(configuration = configurationWithExpiredSession) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                val parameters = it.url.parameters
                assertEquals("refresh_token", parameters["grant_type"])
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRefreshToken, body["refresh_token"]?.jsonPrimitive?.content)
                respondJson(
                    expectedSession
                )
            }.awaitInit()
            client.auth.awaitInitialization()
            client.auth.config.alwaysAutoRefresh = true // this config override is for catching edge cases (like for #1132)
            client.auth.refreshCurrentSession()
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            val status = client.auth.sessionStatus.value as SessionStatus.Authenticated
            assertIs<SessionSource.Refresh>(status.source)
            assertEquals(expectedSession, status.session)
        }
    }

    @Test
    fun testGetClaimsHS256() {
        runTest {
            val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/user", it.url.pathAfterVersion())
                assertEquals("Bearer $jwt", it.headers["Authorization"])
                respondJson(
                    sampleUserObject()
                )
            }.awaitInit()
            val claimsResponse = client.auth.getClaims(jwt) {
                allowExpired = true
            }
            assertEquals("1234567890", claimsResponse.claims.sub)
            assertEquals("John Doe", claimsResponse.claims.getClaim<String>("name"))
            assertEquals(true, claimsResponse.claims.getClaim<Boolean>("admin"))
        }
    }

    @Test
    fun testGetClaimsRS256FetchesJwks() {
        runTest {
            val jwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2V5LWlkIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.dGVzdC1zaWduYXR1cmU"
            var jwksFetched = false
            client = createMockedSupabaseClient(configuration = configuration) {
                when {
                    it.url.encodedPath.contains(".well-known/jwks.json") -> {
                        jwksFetched = true
                        assertMethodIs(HttpMethod.Get, it.method)
                        respondJson(sampleJwksResponse())
                    }
                    else -> respond("")
                }
            }.awaitInit()
            // The signature verification will fail since we're using a dummy signature, but this test verifies the JWKS endpoint is called correctly for RS256
            try {
                client.auth.getClaims(jwt) {
                    allowExpired = true
                }
            } catch (_: Exception) {
                // Expected - signature verification fails with dummy data
            }
            assertEquals(true, jwksFetched, "JWKS endpoint should be fetched for RS256 algorithm")
        }
    }

    @Test
    fun testGetClaimsRejectsHeaderAlgNotMatchingKeyAlg() {
        runTest {
            val jwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2V5LWlkIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.dGVzdC1zaWduYXR1cmU"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/.well-known/jwks.json", it.url.pathAfterVersion())
                respondJson(sampleJwksResponse())
            }.awaitInit()
            assertFailsWith<InvalidJwtException> {
                client.auth.getClaims(jwt) {
                    allowExpired = true
                }
            }
        }
    }

    @Test
    fun testGetClaimsRejectsHeaderAlgNotMatchingKeyType() {
        runTest {
            val jwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3QtZWMta2V5LWlkIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.dGVzdC1zaWduYXR1cmU"
            client = createMockedSupabaseClient(configuration = configuration) {
                assertPathIs("/.well-known/jwks.json", it.url.pathAfterVersion())
                respondJson(
                    """
                    {
                        "keys": [
                            {
                                "kty": "EC",
                                "kid": "test-ec-key-id",
                                "crv": "P-256",
                                "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                                "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0",
                                "key_ops": ["verify"]
                            }
                        ]
                    }
                    """.trimIndent()
                )
            }.awaitInit()
            assertFailsWith<InvalidJwtException> {
                client.auth.getClaims(jwt) {
                    allowExpired = true
                }
            }
        }
    }

    private fun sampleJwksResponse() = """
        {
            "keys": [
                {
                    "kty": "RSA",
                    "kid": "test-key-id",
                    "use": "sig",
                    "alg": "RS256",
                    "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                    "e": "AQAB",
                    "key_ops": ["verify"]
                }
            ]
        }
    """.trimIndent()

    private fun sampleUserObject(email: String? = null, phone: String? = null) = """
        {
            "id": "id",
            "aud": "aud",
            "email": "$email",
            "phone": "$phone"
        }
    """.trimIndent()

    private fun sampleUserSession() = """
        {
        "access_token": "token",
        "refresh_token": "refresh",
        "token_type": "bearer",
        "expires_in": 3600
        }
    """.trimIndent()

    private fun sampleSessionWithUserData(email: String? = null, phone: String? = null) = """
        {   
            "id": "id",
            "aud": "aud",
            "email": "$email",
            "phone": "$phone",
            "access_token": "token",
            "refresh_token": "refresh",
            "token_type": "bearer",
            "expires_in": 3600,
            "user": {
                "id": "id",
                "aud": "aud",
                "email": "$email",
                "phone": "$phone"
            }
        }
    """.trimIndent()

    private fun containsCodeChallenge(body: JsonObject) {
        assertNotNull(body["code_challenge"])
        assertEquals(PKCEConstants.CHALLENGE_METHOD, body["code_challenge_method"]?.jsonPrimitive?.content)
    }

    private suspend fun SupabaseClient.awaitInit(): SupabaseClient {
        auth.awaitInitialization()
        return this
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

}

expect fun AuthConfig.platformSettings()