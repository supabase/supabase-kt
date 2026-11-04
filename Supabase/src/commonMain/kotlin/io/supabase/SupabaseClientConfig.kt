package io.supabase

import io.supabase.logging.LogLevel
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration

data class SupabaseClientConfig(
    val supabaseUrl: String,
    val supabaseKey: String,
    val loggingConfig: SupabaseLoggingConfig,
    val networkConfig: SupabaseNetworkConfig,
    val defaultSerializer: SupabaseSerializer,
    val coroutineDispatcher: CoroutineDispatcher,
    val accessToken: AccessTokenProvider?,
    val plugins: Map<String, PluginProvider>,
    val osInformation: OSInformation?
)

data class SupabaseLoggingConfig(
    val defaultLogLevel: LogLevel,
    val defaultLoggingFactory: SupabaseLoggingProcessorFactory
)

data class SupabaseNetworkConfig(
    val useHTTPS: Boolean,
    val httpEngine: HttpClientEngine?,
    val httpConfigOverrides: List<HttpConfigOverride>,
    val requestTimeout: Duration
)
