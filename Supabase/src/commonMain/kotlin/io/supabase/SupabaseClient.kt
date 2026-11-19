package io.supabase

import io.supabase.annotations.SupabaseInternal
import io.supabase.logging.SupabaseLogger
import io.supabase.logging.createLogger
import io.supabase.logging.i
import io.supabase.logging.w
import io.supabase.network.KtorSupabaseHttpClient
import io.supabase.plugins.PluginManager
import io.supabase.plugins.SupabasePlugin
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The main class to interact with Supabase.
 *
 * To add functionality, add plugins like **Auth** or **Functions** within the [SupabaseClientBuilder]
 */
interface SupabaseClient {

    /**
     * The configuration for the Supabase Client.
     */
    val config: SupabaseClientConfig

    /**
     * The supabase url with either a http or https scheme.
     */
    val supabaseHttpUrl: String

    /**
     * The base supabase url without any scheme
     */
    val supabaseUrl: String

    /**
     * The api key for interacting with the supabase api
     */
    val supabaseKey: String

    /**
     * The plugin manager is used to manage installed plugins
     */
    val pluginManager: PluginManager

    /**
     * The http client used to interact with the Supabase api
     */
    val httpClient: KtorSupabaseHttpClient

    /**
     * Whether [supabaseHttpUrl] uses https
     */
    val useHTTPS: Boolean

    /**
     * The default serializer used to serialize and deserialize custom data types.
     */
    val defaultSerializer: SupabaseSerializer

    /**
     * The custom access token provider used to provide custom access tokens for requests. Configured within the [SupabaseClientBuilder]
     */
    @SupabaseInternal
    val accessToken: AccessTokenProvider?

    /**
     * The default dispatcher used for launching long running jobs
     */
    @SupabaseInternal
    val coroutineDispatcher: CoroutineDispatcher

    val logger: SupabaseLogger

    /**
     * Releases all resources held by the [httpClient] and all plugins the [pluginManager]
     */
    suspend fun close()

    companion object {

        /**
         * The tag for the Supabase logger.
         */
        const val LOGGING_TAG = "Supabase-Core"

        internal const val TEMP_KEY_PREFIX = "sb_temp_"

        @SupabaseInternal
        fun checkIsNewApiKey(key: String) = key.startsWith("sb_publishable_") || key.startsWith("sb_secret_")

    }

}

internal class SupabaseClientImpl(
    override val config: SupabaseClientConfig,
) : SupabaseClient {

    override val accessToken: AccessTokenProvider? = config.accessToken
    override val defaultSerializer: SupabaseSerializer = config.defaultSerializer
    override val supabaseUrl: String = config.supabaseUrl
    override val supabaseKey: String = config.supabaseKey
    override val useHTTPS: Boolean = config.networkConfig.useHTTPS
    override val coroutineDispatcher: CoroutineDispatcher = config.coroutineDispatcher
    override val logger: SupabaseLogger = createLogger(
        SupabaseClient.LOGGING_TAG,
        config.loggingConfig.defaultLogLevel,
        config.loggingConfig.defaultLoggingFactory
    )

    init {
        logger.i {
            "SupabaseClient created! Please report any bugs you find."
        }
        try {
            getOSInformation()
        } catch(e: Exception) {
            logger.i(e) {
                "Failed to get OS information. If this is not intentional, please report this issue."
            }
        }
        checkApiKeyFormat(config.supabaseKey)
    }

    /**
     * Warn (once per subtype) when an `sb_` key isn't a subtype this SDK version recognizes.
     * Never throws — the server, not the SDK, decides key validity. The key value is never
     * included in the message.
     */
    private fun checkApiKeyFormat(supabaseKey: String) {
        if(!supabaseKey.startsWith("sb_") || SupabaseClient.checkIsNewApiKey(supabaseKey) || supabaseKey.startsWith(
                SupabaseClient.TEMP_KEY_PREFIX)) {
            return
        }
        logger.w {
            """
                Unrecognized Supabase API key format. 
                The client will proceed and send this key as-is; 
                if you see authentication errors you may need to upgrade supabase-kt to a version that recognizes this key type.
            """.trimIndent()
        }
    }

    override val supabaseHttpUrl: String = if (useHTTPS) {
        "https://$supabaseUrl"
    } else {
        "http://$supabaseUrl"
    }

    @OptIn(SupabaseInternal::class)
    override val httpClient = KtorSupabaseHttpClient(
        this
    )

    override val pluginManager = PluginManager(config.plugins.toList().associate { (key, value) ->
        key to value(this)
    }, logger.appendTag(" [PluginManager]"))

    init {
        pluginManager.installedPlugins.values.forEach(SupabasePlugin<*>::init)
    }

    override suspend fun close() {
        logger.i { "Closing SupabaseClient" }
        httpClient.close()
        pluginManager.closeAllPlugins()
    }

}

