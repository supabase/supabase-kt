package io.supabase.auth.providers.builtin

import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.AuthImpl
import io.supabase.auth.FlowType
import io.supabase.auth.auth
import io.supabase.auth.generateCodeChallenge
import io.supabase.auth.generateCodeVerifier
import io.supabase.auth.providers.AuthProvider
import io.supabase.auth.putCodeChallenge
import io.supabase.auth.redirectTo
import io.supabase.auth.user.UserSession
import io.supabase.logging.w
import io.supabase.putJsonObject
import io.supabase.safeBody
import io.supabase.supabaseJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * A default authentication provider
 * @see Email
 * @see Phone
 * @see IDToken
 */
sealed interface DefaultAuthProvider<C, R> : AuthProvider<C, R> {

    /**
     * The grant type of the provider.
     */
    val grantType: String

    /**
     * The default configuration for the provider.
     * @param captchaToken The captcha token when having captcha enabled
     */
    @Serializable
    sealed class Config(
        @Serializable(with = CaptchaTokenSerializer::class)
        @SerialName("gotrue_meta_security")
        var captchaToken: String? = null,
    )

    override suspend fun login(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ) {
        require(config != null) { "Credentials are required" }
        val encodedCredentials = encodeCredentials(config)
        val gotrue = supabaseClient.auth as AuthImpl
        val url = "token?grant_type=$grantType"
        val response = gotrue.publicApi.postJson(url, encodedCredentials) {
            redirectUrl?.let { redirectTo(it) }
        }
        response.safeBody<UserSession>().also {
            onSuccess(it)
        }
    }

    override suspend fun signUp(
        supabaseClient: SupabaseClient,
        onSuccess: suspend (UserSession) -> Unit,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ): R? {
        require(config != null) { "Credentials are required" }
        val body = encodeCredentials(config)
        val gotrue = supabaseClient.auth as AuthImpl
        var codeChallenge: String? = null
        if(gotrue.config.flowType == FlowType.PKCE) {
            val codeVerifier = generateCodeVerifier()
            gotrue.codeVerifierCache.saveCodeVerifier(codeVerifier)
            codeChallenge = generateCodeChallenge(codeVerifier)
        }
        val url = when (this) {
            Email -> "signup"
            Phone -> "signup"
            IDToken -> "token?grant_type=id_token"
        }
        val response = gotrue.publicApi.postJson(url, buildJsonObject {
            putJsonObject(body)
            if (codeChallenge != null) {
                putCodeChallenge(codeChallenge)
            }
        }) {
            redirectUrl?.let { redirectTo(it) }
        }
        val json = response.safeBody<JsonObject>()
        if (json.containsKey("access_token")) {
            runCatching {
                val userSession = supabaseJson.decodeFromJsonElement<UserSession>(json)
                onSuccess(userSession)
                val userJson = json["user"]?.jsonObject ?: buildJsonObject { }
                return decodeResult(userJson)
            }.onFailure { exception ->
                supabaseClient.auth.logger.w(exception) { "Failed to decode user info" }
                return null
            }
        }
        return decodeResult(json)
    }

    @SupabaseInternal
    fun decodeResult(json: JsonObject): R

    @SupabaseInternal
    fun encodeCredentials(credentials: C.() -> Unit): JsonObject

}