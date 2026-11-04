package io.supabase

import io.supabase.annotations.SupabaseDsl
import io.supabase.annotations.SupabaseInternal
import io.supabase.logging.KermitLoggingProcessor
import io.supabase.logging.LogLevel
import io.supabase.logging.SupabaseLoggingProcessor
import io.supabase.plugins.PluginManager
import io.supabase.plugins.SupabasePlugin
import io.supabase.plugins.SupabasePluginProvider
import io.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal typealias PluginProvider = (SupabaseClient) -> SupabasePlugin<*>
internal typealias HttpConfigOverride = HttpClientConfig<*>.() -> Unit
typealias SupabaseLoggingProcessorFactory = (level: LogLevel) -> SupabaseLoggingProcessor

/**
 * Creates a new [SupabaseClient] with the given options.
 *
 * Use [createSupabaseClient] to create a new instance of [SupabaseClient].
 */
@SupabaseDsl
class SupabaseClientBuilder @PublishedApi internal constructor(
    private val supabaseUrl: String,
    private val supabaseKey: String
) {

    /**
     * Whether to use HTTPS for network requests with the [supabaseUrl]
     *
     * Default: true
     */
    var useHTTPS = true

    /**
     * Custom ktor http client engine. If null, an engine from the dependencies will be used.
     */
    var httpEngine: HttpClientEngine? = null

    /**
     * Whether to ignore if [supabaseUrl] contains modules like 'realtime' or 'auth'. If false, an exception will be thrown.
     *
     * Default: false
     */
    var ignoreModulesInUrl = false

    /**
     * [Duration] after network requests throw a [HttpRequestTimeoutException]
     *
     * Default: 10 seconds
     */
    var requestTimeout = 10.seconds

    /**
     * The default log level used for plugins.
     *
     * Can be overridden by the plugin's config
     */
    var defaultLogLevel: LogLevel = LogLevel.INFO

    /**
     * The default logging factory. Used for creating a [SupabaseLoggingProcessor] using a specified [LogLevel]
     */
    var defaultLoggingFactory: SupabaseLoggingProcessorFactory = { level -> KermitLoggingProcessor(level) }

    /**
     * The default serializer used to serialize and deserialize custom data types.
     *
     * Default: [KotlinXSerializer]
     */
    var defaultSerializer: SupabaseSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })

    /**
     * The CoroutineDispatcher used for launching long running jobs.
     *
     * Default: [defaultDispatcher], the IO Dispatcher on supported targets.
     */
    var coroutineDispatcher: CoroutineDispatcher = defaultDispatcher

    /**
     * Optional function for using a third-party authentication system with
     * Supabase. The function should return an access token or ID token (JWT) by
     * obtaining it from the third-party auth client library. Note that this
     * function may be called concurrently and many times. Use memoization and
     * locking techniques if this is not supported by the client libraries.
     *
     * When set, the Auth plugin from `auth-kt` cannot be used.
     * Create another client if you wish to use Supabase Auth and third-party
     * authentications concurrently in the same application.
     */
    var accessToken: AccessTokenProvider? = null

    /**
     * The current operating system information.
     */
    var osInformation: OSInformation? = OSInformation.CURRENT
    private val httpConfigOverrides = mutableListOf<HttpConfigOverride>()
    private val plugins = mutableMapOf<String, PluginProvider>()

    init {
        val module = when {
            supabaseUrl.contains("realtime/v1") -> "realtime/v1"
            supabaseUrl.contains("auth/v1") -> "auth/v1"
            supabaseUrl.contains("storage/v1") -> "storage/v1"
            supabaseUrl.contains("rest/v1") -> "rest/v1"
            else -> null
        }
        if(!ignoreModulesInUrl && module != null) {
            error("The Supabase URL should not contain ($module), supabase-kt handles the url endpoints. If you want to use a custom url for a module, specify it within their builder but that's not necessary for normal Supabase projects")
        }
        if(supabaseUrl.startsWith("http://")) {
            useHTTPS = false
        }
    }

    @PublishedApi
    internal fun build(): SupabaseClient {
        val config = SupabaseClientConfig(
            supabaseUrl = supabaseUrl.split("//").last(),
            supabaseKey = supabaseKey,
            loggingConfig = SupabaseLoggingConfig(
                defaultLogLevel,
                defaultLoggingFactory
            ),
            networkConfig = SupabaseNetworkConfig(
                useHTTPS = useHTTPS,
                httpEngine = httpEngine,
                httpConfigOverrides = httpConfigOverrides,
                requestTimeout = requestTimeout
            ),
            defaultSerializer = defaultSerializer,
            coroutineDispatcher = coroutineDispatcher,
            accessToken = accessToken,
            plugins = plugins,
            osInformation = osInformation
        )
        return SupabaseClientImpl(
            config
        )
    }

    /**
     * Add your own http configuration to [SupabaseClient.httpClient]
     *
     * **Warning:** This is an internal function and should only be used if you know what you are doing. You don't have to specify the HTTP client engine, it will be automatically set by Ktor.
     */
    @SupabaseInternal
    @SupabaseDsl
    fun httpConfig(block: @SupabaseDsl HttpClientConfig<*>.() -> Unit) {
        httpConfigOverrides.add(block)
    }

    /**
     * Installs a plugin to the [SupabaseClient]
     *
     * Plugins can be either retrieved by calling [PluginManager.getPlugin] within your [SupabaseClient] instance or by using the corresponding **SupabaseClient.plugin** extension property.
     */
    @SupabaseDsl
    fun <Config, PluginInstance : SupabasePlugin<Config>, Provider : SupabasePluginProvider<Config, PluginInstance>> install(plugin: Provider, init: @SupabaseDsl Config.() -> Unit = {}) {
        val config = plugin.createConfig(init)
        plugin.setup(this, config)
        plugins[plugin.key] = {
            plugin.create(it, config)
        }
    }

}

/**
 * Creates a new [SupabaseClient] instance using [builder]
 * @param supabaseUrl The supabase url. Example: 'id.supabase.co' or 'https://id.supabase.co' (no 'https://id.supabase.co/auth/v1')
 * @param supabaseKey The supabase api key
 */
inline fun createSupabaseClient(supabaseUrl: String, supabaseKey: String, builder: SupabaseClientBuilder.() -> Unit) = SupabaseClientBuilder(supabaseUrl, supabaseKey).apply(builder).build()