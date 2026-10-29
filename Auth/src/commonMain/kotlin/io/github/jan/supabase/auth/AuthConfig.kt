package io.github.jan.supabase.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.jwt.JwkCache
import io.github.jan.supabase.auth.jwt.SharedJwkCache
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.plugins.CustomSerializationConfig
import io.github.jan.supabase.plugins.MainConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The configuration for [Auth]
 */
expect class AuthConfig() : AuthConfigDefaults

/**
 * The default values for the [AuthConfig]
 */
open class AuthConfigDefaults : MainConfig(), AuthDependentPluginConfig, CustomSerializationConfig {

    /**
     * The duration after which [Auth] should retry refreshing a session, when it failed due to network issues
     */
    var retryDelay: Duration = 10.seconds

    /**
     * Whether to always automatically refresh the session, when it expires
     */
    var alwaysAutoRefresh: Boolean = true

    /**
     * Whether to automatically load the session from [sessionManager], when [Auth] is initialized
     */
    var autoLoadFromStorage: Boolean = true

    /**
     * Whether to automatically save the session to [sessionManager], when the session changes
     */
    var autoSaveToStorage: Boolean = true

    /**
     * The session manager used to store/load the session. When null, the default [io.github.jan.supabase.auth.SettingsSessionManager] will be used
     */
    var sessionManager: SessionManager? = null

    /**
     * The cache used to store/load the code verifier for the [FlowType.PKCE] flow. When null, the default [io.github.jan.supabase.auth.SettingsCodeVerifierCache] will be used
     */
    var codeVerifierCache: CodeVerifierCache? = null

    /**
     * The dispatcher used for all auth related network requests
     */
    @Deprecated("SupabaseClientBuilder.coroutineDispatcher should be used instead")
    var coroutineDispatcher: CoroutineDispatcher? = null

    @SupabaseInternal
    var authScope: CoroutineScope? = null

    /**
     * The type of login flow to use. Defaults to [FlowType.IMPLICIT]
     */
    var flowType: FlowType = FlowType.IMPLICIT

    /**
     * A serializer used for serializing/deserializing objects e.g. in [Auth.signInWith]. Defaults to [SupabaseClientBuilder.defaultSerializer], when null.
     */
    override var serializer: SupabaseSerializer? = null

    /**
     * The deeplink scheme used for the implicit and PKCE flow. When null, deeplinks won't be used as redirect urls
     *
     * **Note:** Deeplinks are only used as redirect urls on Android and Apple platforms. Other platforms will use their own default redirect url.
     */
    var scheme: String? = null

    /**
     * The deeplink host used for the implicit and PKCE flow. When null, deeplinks won't be used as redirect urls
     *
     * **Note:** Deeplinks are only used as redirect urls on Android and Apple platforms. Other platforms will use their own default redirect url.
     */
    var host: String? = null

    /**
     * The default redirect url used for authentication. When null, a platform specific default redirect url will be used.
     *
     * On Android and Apple platforms, the default redirect url is the deeplink.
     *
     * On Browser platforms, the default redirect url is the current url.
     *
     * On Desktop (excluding MacOS) platforms, there is no default redirect url. For OAuth flows, a http callback server will be used and a localhost url will be the redirect url.
     */
    var defaultRedirectUrl: String? = null

    /**
     * Whether to stop auto-refresh on focus loss, and resume it on focus again.
     *
     * Currently only supported on Android, iOS and tvOS.
     */
    var enableLifecycleCallbacks: Boolean = true

    /**
     * Whether to reset [Auth.sessionStatus] to [SessionStatus.Initializing] when the app moves to
     * the background.
     *
     * When `true` (default), the status is reset on background and re-established on resume, which
     * means consumers observe a transient [SessionStatus.Initializing] after every resume. When
     * `false`, the last known status (e.g. [SessionStatus.Authenticated]) is kept while auto-refresh
     * is paused, avoiding that transient state.
     *
     * Only has an effect when [enableLifecycleCallbacks] is `true`. Currently only supported on Android.
     */
    var resetStatusOnBackground: Boolean = true

    /**
     * The URL launcher used to open OAuth links in the system browser.
     */
    @SupabaseExperimental
    var urlLauncher: UrlLauncher = UrlLauncher.DEFAULT

    /**
     * Whether to automatically set up the current platform. For testing.
     */
    @SupabaseInternal
    var autoSetupPlatform: Boolean = true

    /**
     * Whether to check if the current session is expired on an authenticated request and possibly try to refresh it.
     *
     * **Note: This option is experimental and is a fail-safe for when the auto refresh fails. This option may be removed without notice.**
     */
    @SupabaseExperimental
    var checkSessionOnRequest: Boolean = true

    /**
     * Whether to require a valid [UserSession] in the [Auth] plugin to make any request with this plugin. The [SupabaseClient.supabaseKey] cannot be used as fallback.
     */
    @SupabaseExperimental
    override var requireValidSession: Boolean = false

    /**
     * A [JwkCache] is used to
     */
    var jwkCache: JwkCache = SharedJwkCache

}

/**
 * The type of login flow to use
 */
enum class FlowType {
    /**
     * The implicit flow is the default flow, which is easier to use, but less secure.
     *
     * Note: OTP's via a link and sign up verification links are not supported on desktop. Replace your email template to send the token instead.
     */
    IMPLICIT,

    /**
     * The PKCE flow is more secure, as it uses a code verifier to exchange the code for a session making it harder to intercept the session
     *
     * Note: OTP's via a link and sign up verification links are not supported on desktop. Replace your email template to send the token instead.
     */
    PKCE
}

/**
 * The deeplink used for the implicit and PKCE flow. Throws an [IllegalArgumentException], if either the scheme or host is not set
 */
@Suppress("unused")
val AuthConfig.deepLink: String
    get() {
        val scheme = scheme ?: noDeeplinkError("scheme")
        val host = host ?: noDeeplinkError("host")
        return "${scheme}://${host}"
    }

/**
 * The deeplink used for the implicit and PKCE flow. Returns null, if either the scheme or host is not set
 */
val AuthConfig.deepLinkOrNull: String?
    get() {
        val scheme = scheme ?: return null
        val host = host ?: return null
        return "${scheme}://${host}"
    }

/**
 * Applies minimal settings to the [AuthConfig]. This is useful for server side applications, where you don't need to store the session or code verifier.
 * @param alwaysAutoRefresh Whether to always automatically refresh the session, when it expires
 * @param autoLoadFromStorage Whether to automatically load the session from [sessionManager], when [Auth] is initialized
 * @param autoSaveToStorage Whether to automatically save the session to [sessionManager], when the session changes
 * @param sessionManager The session manager used to store/load the session.
 * @param codeVerifierCache The cache used to store/load the code verifier for the [FlowType.PKCE] flow.
 * @param enableLifecycleCallbacks Whether to stop auto-refresh on focus loss, and resume it on focus again. Currently only supported on Android, iOS and tvOS.
 * @see AuthConfigDefaults
 */
@Deprecated("Use the new minimalConfig function instead", ReplaceWith("minimalConfig()"))
@Suppress("LongParameterList", "unused")
fun AuthConfigDefaults.minimalSettings(
    alwaysAutoRefresh: Boolean = false,
    autoLoadFromStorage: Boolean = false,
    autoSaveToStorage: Boolean = false,
    sessionManager: SessionManager? = MemorySessionManager(),
    codeVerifierCache: CodeVerifierCache? = MemoryCodeVerifierCache(),
    enableLifecycleCallbacks: Boolean = false
) {
    this.alwaysAutoRefresh = alwaysAutoRefresh
    this.autoLoadFromStorage = autoLoadFromStorage
    this.autoSaveToStorage = autoSaveToStorage
    this.sessionManager = sessionManager
    this.codeVerifierCache = codeVerifierCache
    this.enableLifecycleCallbacks = enableLifecycleCallbacks
}