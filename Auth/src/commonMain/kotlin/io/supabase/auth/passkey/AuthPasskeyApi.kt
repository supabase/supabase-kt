package io.supabase.auth.passkey

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.auth.user.UserSession
import io.supabase.safeBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Interface for interacting with the Supabase Passkey API
 */
interface AuthPasskeyApi {

    /**
     * Start passkey registration for the current authenticated user.
     */
    suspend fun startRegistration(): PasskeyRegistrationResponse

    /**
     * Verify passkey registration with the credential response.
     * @param challengeId Challenge ID from startRegistration
     * @param credential Serialized credential
     */
    suspend fun verifyRegistration(challengeId: String, credential: String): PasskeyRegistrationVerifyResponse

    /**
     * Start passkey authentication.
     * @param builder Extra parameters like a captcha token
     */
    suspend fun startAuthentication(builder: PasskeyAuthenticationBuilder.() -> Unit = {}): PasskeyAuthenticationOptionsResponse

    /**
     * Verify passkey authentication and create a session.
     * @param challengeId Challenge ID from startAuthentication
     * @param credential Serialized credential
     */
    suspend fun verifyAuthentication(challengeId: String, credential: String): UserSession

    /**
     * List all passkeys for the current user.
     */
    suspend fun list(): List<PasskeyListItem>

    /**
     * Update a passkey.
     * @param passkeyId UUID of the passkey to delete
     */
    suspend fun delete(passkeyId: String)

    /**
     * Delete a passkey.
     * @param passkeyId UUID of the passkey to update
     * @param friendlyName New friendly name (max 120 chars)
     */
    suspend fun update(passkeyId: String, friendlyName: String): PasskeyListItem

}

internal class AuthPasskeyApiImpl(
    private val api: AuthenticatedSupabaseApi,
    private val saveSession: suspend (UserSession) -> Unit,
): AuthPasskeyApi {

    override suspend fun startRegistration(): PasskeyRegistrationResponse {
        val result = api.post("registration/options")
        return result.safeBody()
    }

    override suspend fun verifyRegistration(
        challengeId: String,
        credential: String
    ): PasskeyRegistrationVerifyResponse {
        return api.postJson("registration/verify", buildJsonObject {
            put("challenge_id", challengeId)
            put("credential", Json.decodeFromString(credential))
        }).safeBody()
    }

    override suspend fun startAuthentication(builder: PasskeyAuthenticationBuilder.() -> Unit): PasskeyAuthenticationOptionsResponse {
        return api.postJson("authentication/options", buildJsonObject {
            val builder = PasskeyAuthenticationBuilder().apply(builder)
            builder.captchaToken?.let {
                putJsonObject("gotrue_meta_security") {
                    put("captcha_token", it)
                }
            }
        }).safeBody()
    }

    override suspend fun verifyAuthentication(
        challengeId: String,
        credential: String
    ): UserSession {
        return api.postJson("authentication/verify", buildJsonObject {
            put("challenge_id", challengeId)
            put("credential", Json.decodeFromString(credential))
        }).safeBody<UserSession>().also {
            saveSession(it)
        }
    }

    override suspend fun list(): List<PasskeyListItem> {
        return api.get("").safeBody()
    }

    override suspend fun delete(passkeyId: String) {
        api.delete(passkeyId)
    }

    override suspend fun update(
        passkeyId: String,
        friendlyName: String
    ): PasskeyListItem {
        return api.patchJson(passkeyId, buildJsonObject {
            put("friendly_name", friendlyName)
        }).safeBody()
    }


}