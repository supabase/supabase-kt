@file:Suppress("LargeClass")
package io.github.jan.supabase.auth

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.admin.AdminApi
import io.github.jan.supabase.auth.admin.AdminApiImpl
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.exception.InvalidJwtException
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.exception.TokenExpiredException
import io.github.jan.supabase.auth.jwt.ClaimsRequestBuilder
import io.github.jan.supabase.auth.jwt.ClaimsResponse
import io.github.jan.supabase.auth.jwt.JWK
import io.github.jan.supabase.auth.jwt.JWTUtils
import io.github.jan.supabase.auth.jwt.JwkCacheEntry
import io.github.jan.supabase.auth.jwt.JwtHeader
import io.github.jan.supabase.auth.jwt.ecJwkToDer
import io.github.jan.supabase.auth.jwt.ecdsaRawToDer
import io.github.jan.supabase.auth.jwt.rsaJwkToDer
import io.github.jan.supabase.auth.mfa.MfaApi
import io.github.jan.supabase.auth.mfa.MfaApiImpl
import io.github.jan.supabase.auth.oauth.OAuthApi
import io.github.jan.supabase.auth.oauth.OAuthApiImpl
import io.github.jan.supabase.auth.passkey.AuthPasskeyApi
import io.github.jan.supabase.auth.passkey.AuthPasskeyApiImpl
import io.github.jan.supabase.auth.providers.AuthProvider
import io.github.jan.supabase.auth.providers.ExternalAuthConfigDefaults
import io.github.jan.supabase.auth.providers.IDTokenProvider
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.SSO
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.auth.user.UserUpdateBuilder
import io.github.jan.supabase.bodyOrNull
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.logging.SupabaseLogger
import io.github.jan.supabase.logging.createLogger
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.e
import io.github.jan.supabase.logging.i
import io.github.jan.supabase.network.supabaseApi
import io.github.jan.supabase.putJsonObject
import io.github.jan.supabase.safeBody
import io.github.jan.supabase.supabaseJson
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SESSION_REFRESH_THRESHOLD = 0.8
private val JWKS_TTL = 10.minutes
@Suppress("MagicNumber") // see #631
private val SIGN_OUT_IGNORE_CODES = listOf(401, 403, 404)
@Suppress("MagicNumber") // see #1260
private val NETWORK_ERROR_CODES = listOf(500, 502, 503, 504, 520, 521, 522, 523, 524, 530)

@PublishedApi
internal class AuthImpl(
    override val supabaseClient: SupabaseClient,
    override val config: AuthConfig
) : Auth {

    override val logger: SupabaseLogger = supabaseClient.createLogger(Auth.LOGGING_TAG, config)
    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    override val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()
    private val _events = MutableSharedFlow<AuthEvent>(replay = 1)
    override val events: SharedFlow<AuthEvent> = _events.asSharedFlow()
    @Suppress("DEPRECATION")
    override val authScope = config.authScope ?: CoroutineScope((config.coroutineDispatcher ?: supabaseClient.coroutineDispatcher) + SupervisorJob())
    override val sessionManager = config.sessionManager ?: createDefaultSessionManager()
    override val codeVerifierCache = config.codeVerifierCache ?: createDefaultCodeVerifierCache()

    internal val publicApi = supabaseClient.authenticatedSupabaseApi(this, requireSession = false)
    @OptIn(SupabaseInternal::class)
    internal val unauthenticatedApi = supabaseClient.supabaseApi(this)
    @OptIn(SupabaseInternal::class)
    internal val userApi = if(config.requireValidSession) supabaseClient.authenticatedSupabaseApi(this) else publicApi
    override val admin: AdminApi = AdminApiImpl(publicApi)
    override val mfa: MfaApi = MfaApiImpl(userApi.resolve("factors"), this)
    override val passkeys: AuthPasskeyApi = AuthPasskeyApiImpl(userApi.resolve("passkeys")) {
        importSession(it)
    }
    override val oauth: OAuthApi = OAuthApiImpl(userApi)
    var sessionJob: Job? = null
    var refreshInformation: SessionRefreshInformation? = null
    override val isAutoRefreshRunning: Boolean
        get() = sessionJob?.isActive == true

    override val serializer = config.serializer ?: supabaseClient.defaultSerializer

    override val apiVersion: Int
        get() = Auth.API_VERSION

    override val pluginKey: String
        get() = Auth.key

    init {
        if (supabaseClient.accessToken != null) error("The Auth plugin is not available when using a custom access token provider. Please uninstall the Auth plugin.")
    }

    override fun init() {
        logger.d { "Initializing Auth plugin..." }
        if (config.autoLoadFromStorage) {
            authScope.launch {
                logger.i {
                    "Loading session from storage..."
                }
                val successful = loadFromStorage(initializing = true)
                if (successful) {
                    logger.i {
                        "Successfully loaded session from storage!"
                    }
                } else {
                    logger.i {
                        "No session found in storage."
                    }
                }
                if(config.autoSetupPlatform) {
                    setupPlatform()
                }
            }
        } else {
            logger.d { "Skipping loading from storage (autoLoadFromStorage is set to false)" }
            if(config.autoSetupPlatform) {
                authScope.launch {
                    setupPlatform()
                }
            }
        }

        logger.d { "Initialized Auth plugin" }
    }

    override suspend fun <C, R, Provider : AuthProvider<C, R>> signInWith(
        provider: Provider,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ) = provider.login(supabaseClient, {
        importSession(it, source = SessionSource.SignIn(provider))
    }, redirectUrl, config)

    override suspend fun signInAnonymously(data: JsonObject?, captchaToken: String?) {
        val response = publicApi.postJson("signup", buildJsonObject {
            data?.let { put("data", it) }
            captchaToken?.let(::putCaptchaToken)
        })
        val session = response.safeBody<UserSession>()
        importSession(session, source = SessionSource.AnonymousSignIn)
    }

    override suspend fun <C, R, Provider : AuthProvider<C, R>> signUpWith(
        provider: Provider,
        redirectUrl: String?,
        config: (C.() -> Unit)?
    ): R? = provider.signUp(supabaseClient, {
        importSession(it, source = SessionSource.SignUp(provider))
    }, redirectUrl, config)

    override suspend fun linkIdentity(
        provider: OAuthProvider,
        redirectUrl: String?,
        config: ExternalAuthConfigDefaults.() -> Unit
    ): String? {
        val automaticallyOpen = ExternalAuthConfigDefaults().apply(config).automaticallyOpenUrl
        val fetchUrl: suspend (String?) -> String = { redirectTo: String? ->
            val url = getOAuthUrl(provider, redirectTo, "user/identities/authorize", config)
            val response = userApi.rawRequest(url) {
                method = HttpMethod.Get
                parameter("skip_http_redirect", true)
            }
            response.safeBody<JsonObject>()["url"]?.jsonPrimitive?.contentOrNull ?: error("No URL found in response")
        }
        if(!automaticallyOpen) {
            return fetchUrl(redirectUrl ?: "")
        }
        startExternalAuth(
            redirectUrl = redirectUrl,
            getUrl = {
                fetchUrl(it)
            },
            onSessionSuccess = {
                importSession(it, source = SessionSource.UserIdentitiesChanged(it))
            }
        )
        return null
    }

    override suspend fun linkIdentityWithIdToken(
        provider: IDTokenProvider,
        idToken: String,
        config: (IDToken.Config) -> Unit
    ) {
        val body = IDToken.Config(idToken = idToken, provider = provider, linkIdentity = true).apply(config)
        val result = userApi.postJson("token?grant_type=id_token", body)
        importSession(result.safeBody(), source = SessionSource.UserIdentitiesChanged(result.safeBody()))
    }

    override suspend fun unlinkIdentity(identityId: String, updateLocalUser: Boolean) {
        userApi.delete("user/identities/$identityId")
        if (updateLocalUser) {
            val session = currentSessionOrNull() ?: return
            val newUser = session.user?.copy(identities = session.user.identities?.filter { it.identityId != identityId })
            val newSession = session.copy(user = newUser)
            setSessionStatus(SessionStatus.Authenticated(newSession, SessionSource.UserIdentitiesChanged(session)))
        }
    }

    override suspend fun retrieveSSOUrl(
        redirectUrl: String?,
        config: SSO.Config.() -> Unit
    ): SSO.Result {
        val createdConfig = SSO.Config().apply(config)

        require((createdConfig.domain != null && createdConfig.domain!!.isNotBlank()) || (createdConfig.providerId != null && createdConfig.providerId!!.isNotBlank())) {
            "Either domain or providerId must be set"
        }

        require(createdConfig.domain == null || createdConfig.providerId == null) {
            "Either domain or providerId must be set, not both"
        }

        val codeChallenge: String? = preparePKCEIfEnabled()
        return publicApi.postJson("sso", buildJsonObject {
            redirectUrl?.let { put("redirect_to", it) }
            createdConfig.captchaToken?.let(::putCaptchaToken)
            codeChallenge?.let(::putCodeChallenge)
            createdConfig.domain?.let {
                put("domain", it)
            }
            createdConfig.providerId?.let {
                put("provider_id", it)
            }
        })
            .safeBody()
    }

    override suspend fun updateUser(
        updateCurrentUser: Boolean,
        redirectUrl: String?,
        config: UserUpdateBuilder.() -> Unit
    ): UserInfo {
        val updateBuilder = UserUpdateBuilder(serializer = serializer).apply(config)
        val codeChallenge = preparePKCEIfEnabled()
        val body = buildJsonObject {
            putJsonObject(supabaseJson.encodeToJsonElement(updateBuilder).jsonObject)
            codeChallenge?.let(::putCodeChallenge)
        }.toString()
        val response = userApi.putJson("user", body) {
            redirectUrl?.let { url.parameters.append("redirect_to", it) }
        }
        val userInfo = response.safeBody<UserInfo>()
        if (updateCurrentUser && sessionStatus.value is SessionStatus.Authenticated) {
            val newSession =
                (sessionStatus.value as SessionStatus.Authenticated).session.copy(user = userInfo)
            if (this.config.autoSaveToStorage) {
                sessionManager.saveSession(newSession)
            }
            setSessionStatus(SessionStatus.Authenticated(newSession, SessionSource.UserChanged(newSession)))
        }
        return userInfo
    }

    private suspend fun resend(type: String,  redirectUrl: String? = null, body: JsonObjectBuilder.() -> Unit) {
        userApi.postJson("resend", buildJsonObject {
            put("type", type)
            val innerBody = buildJsonObject(body)
            putJsonObject(innerBody)
            if(config.flowType == FlowType.PKCE && !innerBody.containsKey("phone")) preparePKCEIfEnabled()?.let(::putCodeChallenge)
        }) {
            redirectUrl?.let { url.parameters["redirect_to"] = it }
        }
    }

    override suspend fun resendEmail(type: OtpType.Email, email: String, captchaToken: String?,  redirectUrl: String?) =
        resend(type = type.type, redirectUrl = redirectUrl) {
            put("email", email)
            captchaToken?.let(::putCaptchaToken)
        }

    override suspend fun resendPhone(
        type: OtpType.Phone,
        phone: String,
        captchaToken: String?
    ) = resend(type.type) {
        put("phone", phone)
        captchaToken?.let(::putCaptchaToken)
    }

    override suspend fun resetPasswordForEmail(
        email: String,
        redirectUrl: String?,
        captchaToken: String?
    ) {
        require(email.isNotBlank()) {
            "Email must not be blank"
        }
        val codeChallenge = preparePKCEIfEnabled()
        val body = buildJsonObject {
            put("email", email)
            captchaToken?.let(::putCaptchaToken)
            codeChallenge?.let(::putCodeChallenge)
        }.toString()
        publicApi.postJson("recover", body) {
            redirectUrl?.let { url.parameters.append("redirect_to", it) }
        }
    }

    override suspend fun reauthenticate() {
        userApi.get("reauthenticate")
    }

    override suspend fun signOut(scope: SignOutScope) {
        val clearIfValidScope = suspend {
            if(scope != SignOutScope.OTHERS) {
                clearSession()
                logger.d { "Session data cleared" }
            } else logger.d { "Skipping clearing session data as sign out scope is 'OTHERS'" }
        }
        if(currentSessionOrNull() == null) {
            logger.i { "Skipping session logout as there is no session available." }
            clearIfValidScope()
            return
        }
        try {
            userApi.post("logout") {
                parameter("scope", scope.name.lowercase())
            }
        } catch(e: RestException) {
            if(e.statusCode in SIGN_OUT_IGNORE_CODES) {
                logger.d { "Received error code ${e.statusCode} while signing out user. This can happen if the user doesn't exist anymore or the JWT is invalid/expired. Proceeding to clean up local data..." }
            } else {
                clearIfValidScope()
                throw e
            }
        }
        logger.d { "Logged out session in Supabase" }
        clearIfValidScope()
    }

    private suspend fun verify(
        type: String,
        token: String?,
        captchaToken: String?,
        additionalData: JsonObjectBuilder.() -> Unit
    ): OtpVerifyResult {
        val body = buildJsonObject {
            put("type", type)
            token?.let { put("token", it) }
            captchaToken?.let(::putCaptchaToken)
            additionalData()
        }
        val response = publicApi.postJson("verify", body)
        val session = supabaseClient.bodyOrNull<UserSession>(response)
        if(session == null) {
            logger.d { "Received `verifyOtp` response without session: ${response.bodyAsText()}. This may occur if changing the email with 'Secure email change' enabled" }
            return OtpVerifyResult.VerifiedNoSession
        }
        importSession(session, source = SessionSource.SignIn(OTP))
        return OtpVerifyResult.Authenticated(session)
    }

    override suspend fun verifyEmailOtp(
        type: OtpType.Email,
        email: String,
        token: String,
        captchaToken: String?
    ) = verify(type.type, token, captchaToken) {
        put("email", email)
    }

    override suspend fun verifyEmailOtp(
        type: OtpType.Email,
        tokenHash: String,
        captchaToken: String?
    ) = verify(type.type, null, captchaToken) {
        put("token_hash", tokenHash)
    }

    override suspend fun verifyPhoneOtp(
        type: OtpType.Phone,
        phone: String,
        token: String,
        captchaToken: String?
    )  {
        verify(type.type, token, captchaToken) {
            put("phone", phone)
        }
    }

    override suspend fun getClaims(jwt: String?, options: ClaimsRequestBuilder.() -> Unit): ClaimsResponse {
        val token = jwt ?: currentAccessTokenOrNull() ?: error("No access token found")
        val (claims, rawHeader, rawPayload) = JWTUtils.decodeJwt(token)
        val options = ClaimsRequestBuilder().apply(options)

        if(!options.allowExpired) {
            val exp = claims.claims.exp
            val now = Clock.System.now()
            if(exp == null || exp < now) throw TokenExpiredException()
        }

        val signingKey = if(claims.header.alg == JwtHeader.Algorithm.HS256 || claims.header.kid == null) null else {
            fetchJwk(claims.header.kid, options.jwks)
        }

        if(signingKey == null) {
            retrieveUser(token)
            return claims
        } else {
            if(!signingKey.supports(claims.header.alg)) throw InvalidJwtException()
            val signedData = "$rawHeader.$rawPayload".encodeToByteArray()
            val verified = when(val alg = claims.header.alg) {
                JwtHeader.Algorithm.RS256 -> {
                    val keyDecoder = CryptographyProvider.Default
                        .get(RSA.PKCS1).publicKeyDecoder(SHA256)
                    val derKey = rsaJwkToDer(signingKey)
                    val key = keyDecoder.decodeFromByteString(RSA.PublicKey.Format.DER, ByteString(derKey))
                    key.signatureVerifier().tryVerifySignature(signedData, claims.signature)
                }
                JwtHeader.Algorithm.ES256 -> {
                    val keyDecoder = CryptographyProvider.Default
                        .get(ECDSA).publicKeyDecoder(EC.Curve.P256)
                    val derKey = ecJwkToDer(signingKey)
                    val key = keyDecoder.decodeFromByteString(EC.PublicKey.Format.DER, ByteString(derKey))
                    val derSignature = ecdsaRawToDer(claims.signature)
                    key.signatureVerifier(SHA256, ECDSA.SignatureFormat.DER).tryVerifySignature(signedData, derSignature)
                }
                else -> error("Invalid alg claim $alg")
            }
            if(!verified) throw InvalidJwtException()
            return claims
        }
    }

    private fun JWK.supports(alg: JwtHeader.Algorithm): Boolean = this.alg?.let { it == alg.name } ?: when(alg) {
        JwtHeader.Algorithm.RS256 -> kty == JWK.KeyType.RSA
        JwtHeader.Algorithm.ES256 -> kty == JWK.KeyType.EC && getParamOrNull<String>("crv") == "P-256"
        JwtHeader.Algorithm.HS256 -> false
    }

    private suspend fun fetchJwk(kid: String, jwks: List<JWK>): JWK? {
        jwks.find { it.kid == kid }?.let { return it } // try to fetch in the supplied jwks
        val now = Clock.System.now()
        config.jwkCache.get()?.let { entry -> // try to fetch from local cache
            val jwk = entry.jwks.find { jwk -> jwk.kid == kid }
            if(jwk != null && entry.cachedAt + JWKS_TTL > now) return jwk
        }
        val response = unauthenticatedApi.get(".well-known/jwks.json").safeBody<JsonObject>() // fetch from the api
        val keysArray = response["keys"]?.jsonArray
        if(!response.containsKey("keys") || keysArray?.isEmpty() ?: true) return null
        val keys = keysArray.map { JWK(it.jsonObject) }
        config.jwkCache.set(JwkCacheEntry(
            keys,
            now
        ))
        val key = keys.find { it.kid == kid }
        return key
    }

    override suspend fun retrieveUser(jwt: String): UserInfo {
        val response = userApi.get("user") {
            headers["Authorization"] = "Bearer $jwt"
        }
        val body = response.bodyAsText()
        return supabaseJson.decodeFromString(body)
    }

    override suspend fun retrieveUserForCurrentSession(updateSession: Boolean): UserInfo {
        val user = retrieveUser(currentAccessTokenOrNull() ?: error("No session found"))
        if (updateSession) {
            val session = currentSessionOrNull() ?: error("No session found")
            val newStatus = SessionStatus.Authenticated(session.copy(user = user), SessionSource.UserChanged(currentSessionOrNull() ?: error("Session shouldn't be null")))
            setSessionStatus(newStatus)
            if (config.autoSaveToStorage) sessionManager.saveSession(newStatus.session)
        }
        return user
    }

    override suspend fun exchangeCodeForSession(code: String, saveSession: Boolean): UserSession {
        val codeVerifier = codeVerifierCache.loadCodeVerifier()
        require(codeVerifier != null) {
            "No code verifier stored. Make sure to use `getOAuthUrl` for the OAuth Url to prepare the PKCE flow."
        }
        val session = unauthenticatedApi.postJson("token?grant_type=pkce", buildJsonObject {
            put("auth_code", code)
            put("code_verifier", codeVerifier)
        }).safeBody<UserSession>()
        codeVerifierCache.deleteCodeVerifier()
        if (saveSession) {
            importSession(session, source = SessionSource.External)
        }
        return session
    }

    override suspend fun refreshSession(refreshToken: String): UserSession {
        logger.d {
            "Refreshing session"
        }
        val body = buildJsonObject {
            put("refresh_token", refreshToken)
        }
        val response = unauthenticatedApi.postJson("token?grant_type=refresh_token", body)
        return response.safeBody("Auth#refreshSession")
    }

    override suspend fun refreshCurrentSession() {
        val newSession = refreshSession(
            currentSessionOrNull()?.refreshToken
                ?: error("No refresh token found in current session")
        )
        importSession(newSession, source = SessionSource.Refresh(currentSessionOrNull() ?: error("No session found")))
        updateRefreshInformation(null, Clock.System.now())
    }

    override suspend fun importSession(
        session: UserSession,
        autoRefresh: Boolean,
        source: SessionSource
    ) = importSession(session, autoRefresh, source, false)

    suspend fun importSession(
        session: UserSession,
        autoRefresh: Boolean,
        source: SessionSource,
        initializing: Boolean
    ) {
        logger.d { "Importing session $session from $source, auto refresh is set to $autoRefresh." }
        if (!autoRefresh) {
            if (session.refreshToken.isNotBlank() && session.expiresIn != 0L && config.autoSaveToStorage) {
                sessionManager.saveSession(session)
                logger.d { "Session saved to storage (no auto refresh)" }
            }
            setSessionStatus(SessionStatus.Authenticated(session, source))
            logger.d { "Session imported successfully." }
            return
        }
        val thresholdDate = session.expiresAt - session.expiresIn.seconds * (1 - SESSION_REFRESH_THRESHOLD)
        if (thresholdDate <= Clock.System.now()) {
            logger.d { "Session is under the threshold date. Refreshing session..." }
            recreateSessionJob(session, source, false, initializing)
        } else {
            if (config.autoSaveToStorage) {
                sessionManager.saveSession(session)
                logger.d { "Session saved to storage (auto refresh enabled)" }
            }
            setSessionStatus(SessionStatus.Authenticated(session, source))
            logger.d { "Session imported successfully. Starting auto refresh..." }
            recreateSessionJob(session, source, true, initializing)
            logger.d { "Auto refresh started." }
        }
    }

    private suspend fun recreateSessionJob(
        session: UserSession,
        source: SessionSource,
        delay: Boolean,
        initializing: Boolean
    ) {
        sessionJob?.cancel()
        sessionJob = authScope.launch {
            if(delay) delayBeforeExpiry(session)
            tryImportingSession(
                { handleExpiredSession(session, config.alwaysAutoRefresh) },
                { importSession(session, source = source) },
                { updateStatusIfExpired(session, it) }
            )
        }
        if (initializing) sessionJob?.join()
    }

    @Suppress("MagicNumber")
    private suspend fun tryImportingSession(
        importRefreshedSession: suspend () -> Unit,
        retry: suspend () -> Unit,
        updateStatus: suspend (RefreshFailureCause) -> Unit
    ) {
        try {
            importRefreshedSession()
        } catch (e: RestException) {
            if (e.statusCode in NETWORK_ERROR_CODES) {
                logger.e(e) { "Couldn't refresh session due to an internal server error. Retrying in ${config.retryDelay} (Status code ${e.statusCode})..." }
                updateStatus(RefreshFailureCause.InternalServerError(e))
                delay(config.retryDelay)
                retry()
            } else {
                logger.e(e) { "Couldn't refresh session. The refresh token may have been revoked. Clearing session (Status code ${e.statusCode})... " }
                clearSession()
            }
        } catch (e: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.d(e) { "Couldn't reach Supabase. Either the address doesn't exist or the network might not be on. Retrying in ${config.retryDelay}..." }
            updateStatus(RefreshFailureCause.NetworkError(e))
            delay(config.retryDelay)
            retry()
        }
    }

    private fun updateStatusIfExpired(session: UserSession, reason: RefreshFailureCause) {
        if (session.expiresAt <= Clock.System.now()) {
            logger.d { "Session expired while trying to refresh the session. Updating status..." }
            setSessionStatus(SessionStatus.RefreshFailure(reason))
        }
        emitEvent(AuthEvent.RefreshFailure(reason))
    }

    private suspend fun delayBeforeExpiry(session: UserSession) {
        val now = Clock.System.now()
        val timeAtBeginningOfSession = session.expiresAt - session.expiresIn.seconds

        // 80% of the way to session.expiresAt
        val targetRefreshTime = timeAtBeginningOfSession + (session.expiresIn.seconds * SESSION_REFRESH_THRESHOLD)

        val delayDuration = targetRefreshTime - now
        updateRefreshInformation(targetRefreshTime, null)
        logger.d {
            "Refreshing session in $delayDuration."
        }
        // if the delayDuration is negative, delay() will not delay
        delay(delayDuration)
    }

    private fun updateRefreshInformation(
        refreshingAt: Instant?,
        lastRefreshedAt: Instant?,
    ) {
        refreshInformation = refreshInformation?.copy(refreshingAt = refreshingAt ?: refreshInformation?.refreshingAt, lastRefreshedAt = lastRefreshedAt ?: refreshInformation?.lastRefreshedAt)
            ?: SessionRefreshInformation(
                Clock.System.now(),
                lastRefreshedAt,
                refreshingAt
            )
    }

    private suspend fun handleExpiredSession(session: UserSession, autoRefresh: Boolean = true) {
        logger.d {
            "Session expired. Refreshing session..."
        }
        val newSession = refreshSession(session.refreshToken)
        importSession(newSession, autoRefresh, SessionSource.Refresh(session))
    }

    override suspend fun startAutoRefreshForCurrentSession() =
        importSession(
            currentSessionOrNull() ?: error("No session found"),
            true,
            (sessionStatus.value as SessionStatus.Authenticated).source
        )

    override fun stopAutoRefreshForCurrentSession() {
        logger.d { "Stopping auto refresh for current session" }
        sessionJob?.cancel()
        sessionJob = null
    }

    @SupabaseInternal
    override fun autoRefreshInformation(): SessionRefreshInformation? = refreshInformation

    override suspend fun loadFromStorage(autoRefresh: Boolean): Boolean = loadFromStorage(autoRefresh, false)

    suspend fun loadFromStorage(autoRefresh: Boolean = config.alwaysAutoRefresh, initializing: Boolean): Boolean {
        val session = try {
            sessionManager.loadSession()
        } catch (e: NoSessionFoundException) {
            logger.d { e.message ?: "No session found in storage" }
            null
        } catch (e: Throwable) {
            currentCoroutineContext().ensureActive()
            logger.e(e) { "Failed to load session" }
            null
        }
        session?.let {
            importSession(it, autoRefresh, SessionSource.Storage, initializing)
        }
        return session != null
    }

    override suspend fun close() {
        authScope.cancel()
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        val errorBody =
            supabaseClient.bodyOrNull<GoTrueErrorResponse>(response) ?: GoTrueErrorResponse("Unknown error", response.status.description)
        checkErrorCodes(errorBody, response)?.let { return it }
        return when (response.status) {
            HttpStatusCode.Unauthorized -> UnauthorizedRestException(
                errorBody.error ?: "Unauthorized",
                response,
                errorBody.description
            )
            HttpStatusCode.BadRequest -> BadRequestRestException(
                errorBody.error ?: "Bad Request",
                response,
                errorBody.description
            )
            HttpStatusCode.UnprocessableEntity -> BadRequestRestException(
                errorBody.error ?: "Unprocessable Entity",
                response,
                errorBody.description
            )
            else -> UnknownRestException(errorBody.error ?: "Unknown Error", response)
        }
    }

    private fun checkErrorCodes(error: GoTrueErrorResponse, response: HttpResponse): RestException? {
        return when (error.error) {
            AuthWeakPasswordException.CODE -> AuthWeakPasswordException(error.description, response, error.weakPassword?.reasons ?: emptyList())
            AuthSessionMissingException.CODE -> {
                authScope.launch {
                    logger.e { "Received session not found api error. Clearing session..." }
                    clearSession()
                }
                AuthSessionMissingException(response)
            }
            else -> {
                error.error?.let { AuthRestException(it, error.description, response) }
            }
        }
    }

    @OptIn(SupabaseExperimental::class)
    override fun getOAuthUrl(
        provider: OAuthProvider,
        redirectUrl: String?,
        url: String,
        additionalConfig: ExternalAuthConfigDefaults.() -> Unit
    ): String {
        val config = ExternalAuthConfigDefaults().apply(additionalConfig)
        val codeChallenge = preparePKCEIfEnabled()
        codeChallenge?.let {
            config.queryParams["code_challenge"] = it
            config.queryParams["code_challenge_method"] = PKCEConstants.CHALLENGE_METHOD
        }
        return resolveUrl(buildString {
            append("$url?provider=${provider.name}&redirect_to=${redirectUrl?.encodeURLParameter()}")
            if (config.scopes.isNotEmpty()) append("&scopes=${config.scopes.joinToString("+")}")
            if (config.queryParams.isNotEmpty()) {
                for ((key, value) in config.queryParams) {
                    append("&$key=${value.encodeURLParameter()}")
                }
            }
        })
    }

    override suspend fun clearSession() {
        codeVerifierCache.deleteCodeVerifier()
        sessionManager.deleteSession()
        setSessionStatus(SessionStatus.NotAuthenticated(true))
        stopAutoRefreshForCurrentSession()
    }

    override suspend fun awaitInitialization() {
        sessionStatus.first { it !is SessionStatus.Initializing }
    }

    override fun setSessionStatus(status: SessionStatus) {
        logger.d { "Setting session status to $status" }
        _sessionStatus.value = status
    }

    override fun emitEvent(event: AuthEvent) {
        logger.d { "Emitting event $event" }
        _events.tryEmit(event)
    }

    /**
     * Prepares PKCE if enabled and returns the code challenge.
     */
    private fun preparePKCEIfEnabled(): String? {
        if (this.config.flowType != FlowType.PKCE) return null
        val codeVerifier = generateCodeVerifier()
        authScope.launch {
            supabaseClient.auth.codeVerifierCache.saveCodeVerifier(codeVerifier)
        }
        return generateCodeChallenge(codeVerifier)
    }

}

@SupabaseInternal
expect suspend fun Auth.setupPlatform()

@SupabaseInternal
expect fun Auth.createDefaultSessionManager(): SessionManager

@SupabaseInternal
expect fun Auth.createDefaultCodeVerifierCache(): CodeVerifierCache