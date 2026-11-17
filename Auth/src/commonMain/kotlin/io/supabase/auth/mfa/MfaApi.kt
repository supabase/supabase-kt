package io.supabase.auth.mfa

import io.supabase.auth.Auth
import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.auth.jwt.JWTUtils
import io.supabase.auth.providers.builtin.Phone
import io.supabase.auth.status.SessionStatus
import io.supabase.auth.user.UserMfaFactor
import io.supabase.auth.user.UserSession
import io.supabase.putJsonObject
import io.supabase.safeBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * An interface for interacting with Multi-Factor Authentication Api in GoTrue.
 */
interface MfaApi {

    /**
     * The current MFA status of the user
     * @see MfaStatus
     */
    val status: MfaStatus

    /**
     * The current MFA status of the user as a flow. This flow will emit a new status any time [Auth.sessionStatus] changes.
     *
     * Note that if the user has verified a factor on another device and the user hasn't been updated on this device, [MfaStatus.active] will return false.
     * You can use [Auth.retrieveUserForCurrentSession] to update the user and this property will update.
     */
    val statusFlow: Flow<MfaStatus>

    /**
     * Returns all verified factors from the current user session. If the user has no verified factors or there is no session, an empty list is returned.
     * To fetch up-to-date factors, use [retrieveFactorsForCurrentUser].
     */
    val verifiedFactors: List<UserMfaFactor>

    /**
     * @param factorType The type of MFA factor to enroll. Currently only supports TOTP.
     * @param friendlyName Human-readable name assigned to a device
     * @return MfaEnrollResponse containing the id of the MFA factor, the type of MFA factor and the data of the MFA factor (like QR-Code for TOTP)
     * @see FactorType
     */
    suspend fun <Config, Response> enroll(factorType: FactorType<Config, Response>, friendlyName: String? = null, config: Config.() -> Unit = {}): MfaFactor<Response>

    /**
     * Unenrolls an MFA factor
     * @param factorId The id of the MFA factor to unenroll
     */
    suspend fun unenroll(factorId: String)

    /**
     * Creates a new MFA challenge, which can be used to verify the user's code using [verifyChallenge]
     * @param factorId The id of the MFA factor to verify
     * @param channel The channel to send the challenge to. Defaults to SMS (only applies to the [FactorType.Phone] factor type)
     */
    suspend fun createChallenge(factorId: String, channel: Phone.Channel? = null): MfaChallenge

    /**
     * Creates a new MFA challenge and immediately verifies it
     * @param factorId The id of the MFA factor to verify
     * @param code The code to verify
     * @param saveSession Whether to save the session after verification in Supabase Auth
     * @param channel The channel to send the challenge to. Defaults to SMS (only applies to the [FactorType.Phone] factor type)
     */
    suspend fun createChallengeAndVerify(factorId: String, code: String, channel: Phone.Channel = Phone.Channel.SMS, saveSession: Boolean = true): UserSession {
        val challenge = createChallenge(factorId, channel)
        // Phone challenges cannot be verified immediately
        if(challenge.factorType == "phone") error("Cannot verify a phone challenge immediately")
        return verifyChallenge(factorId, challenge.id, code, saveSession)
    }

    /**
     * Verifies a MFA challenge
     * @param factorId The id of the MFA factor to verify
     * @param challengeId The id of the challenge to verify
     * @param code The code to verify
     * @param saveSession Whether to save the session after verification in GoTrue
     */
    suspend fun verifyChallenge(factorId: String, challengeId: String, code: String, saveSession: Boolean = true): UserSession

    /**
     * Retrieves all factors for the current user. This will make a network request to fetch the factors. Use [verifiedFactors] to get the factors from the current session.
     */
    suspend fun retrieveFactorsForCurrentUser(): List<UserMfaFactor>

    /**
     * Parses the current JWT and returns the AuthenticatorAssuranceLevel for the current session and the next session
     * @param jwt A custom jwt. If null, the jwt from [Auth.currentAccessTokenOrNull] will be used.
     */
    fun getAuthenticatorAssuranceLevel(jwt: String? = null): MfaLevel

}

@Suppress("DEPRECATION")
internal class MfaApiImpl(
    private val api: AuthenticatedSupabaseApi,
    private val auth: Auth
) : MfaApi {

    override val status: MfaStatus
        get() {
            val (current, next) = getAuthenticatorAssuranceLevel()
            return MfaStatus(
                enabled = next == AuthenticatorAssuranceLevel.AAL2,
                active = current == AuthenticatorAssuranceLevel.AAL2
            )
        }

    override val statusFlow: Flow<MfaStatus> = auth.sessionStatus.map {
        if(it is SessionStatus.Authenticated) {
            val (current, next) = getAuthenticatorAssuranceLevel()
            MfaStatus(
                enabled = next == AuthenticatorAssuranceLevel.AAL2,
                active = current == AuthenticatorAssuranceLevel.AAL2
            )
        } else {
            MfaStatus(enabled = false, active = false)
        }
    }

    override val verifiedFactors: List<UserMfaFactor>
        get() = auth.currentUserOrNull()?.factors?.filter(UserMfaFactor::isVerified) ?: emptyList()

    override suspend fun <Config, Response> enroll(factorType: FactorType<Config, Response>, friendlyName: String?, config: Config.() -> Unit): MfaFactor<Response> {
        val result = api.postJson("", buildJsonObject {
            put("factor_type", factorType.value)
            putJsonObject(factorType.encodeConfig(config))
            friendlyName?.let { put("friendly_name", it) }
        })
        val json = result.safeBody<JsonObject>()
        val factorData = factorType.decodeResponse(json)
        return MfaFactor(
            json["id"]!!.jsonPrimitive.content,
            factorType.value,
            factorData
        )
    }

    override suspend fun createChallenge(factorId: String, channel: Phone.Channel?): MfaChallenge {
        val result = api.postJson("$factorId/challenge", buildJsonObject {
            if (channel != null) {
                put("channel", channel.value)
            }
        })
        return result.safeBody()
    }

    override suspend fun verifyChallenge(
        factorId: String,
        challengeId: String,
        code: String,
        saveSession: Boolean
    ): UserSession {
        val result = api.postJson("$factorId/verify", buildJsonObject {
            put("code", code)
            put("challenge_id", challengeId)
        })
        val session = result.safeBody<UserSession>()
        if(saveSession) {
            auth.importSession(session)
        }
        return session
    }

    override suspend fun unenroll(factorId: String) {
        api.delete("$factorId")
    }

    override fun getAuthenticatorAssuranceLevel(jwt: String?): MfaLevel {
        val jwt = jwt ?: auth.currentAccessTokenOrNull() ?: error("Current session is null")
        val decodedJwt = JWTUtils.decodeJwt(jwt)
        val aal = decodedJwt.claimsResponse.claims.aal ?: error("No 'aal' claim found in JWT")
        val nextAal = if (verifiedFactors.isNotEmpty()) AuthenticatorAssuranceLevel.AAL2 else AuthenticatorAssuranceLevel.AAL1
        return MfaLevel(aal, nextAal, decodedJwt.claimsResponse.claims.amr ?: emptyList())
    }


    override suspend fun retrieveFactorsForCurrentUser(): List<UserMfaFactor> {
        return auth.retrieveUserForCurrentSession().factors
    }

}