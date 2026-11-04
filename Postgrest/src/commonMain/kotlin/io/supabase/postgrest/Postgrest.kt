package io.supabase.postgrest

import io.supabase.SupabaseClient
import io.supabase.SupabaseSerializer
import io.supabase.auth.AuthDependentPluginConfig
import io.supabase.exceptions.HttpRequestException
import io.supabase.plugins.CustomSerializationConfig
import io.supabase.plugins.CustomSerializationPlugin
import io.supabase.plugins.MainConfig
import io.supabase.plugins.MainPlugin
import io.supabase.plugins.SupabasePluginProvider
import io.supabase.postgrest.exception.PostgrestRestException
import io.supabase.postgrest.query.PostgrestQueryBuilder
import io.supabase.postgrest.query.PostgrestRequestBuilder
import io.supabase.postgrest.query.PostgrestUpdate
import io.supabase.postgrest.query.request.RpcRequestBuilder
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Plugin to interact with the supabase Postgrest API
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Postgrest)
 * }
 * ```
 *
 * then you can use it like this:
 * ```kotlin
 * val product = supabase.postgrest["products"].select {
 *    Product::id eq 2
 * }.decodeSingle<Product>()
 * ```
 */

interface Postgrest : MainPlugin<Postgrest.Config>, CustomSerializationPlugin {

    /**
     * Creates a new [PostgrestQueryBuilder] for the given table
     * @param table The table to use for the requests
     */
    fun from(table: String): PostgrestQueryBuilder

    /**
     * Creates a new [PostgrestQueryBuilder] for the given schema and table
     * @param schema The schema to use for the requests
     * @param table The table to use for the requests
     */
    fun from(schema: String, table: String): PostgrestQueryBuilder

    /**
     * Creates a new [PostgrestQueryBuilder] for the given table
     * @param table The table to use for the requests
     */
    operator fun get(schema: String, table: String): PostgrestQueryBuilder = from(schema, table)

    /**
     * Creates a new [PostgrestQueryBuilder] for the given schema and table
     * @param table The table to use for the requests
     */
    operator fun get(table: String): PostgrestQueryBuilder = from(table)

    /**
     * Executes a database function
     *
     * @param function The name of the function
     * @param request Filter the result
     * @throws PostgrestRestException if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun rpc(
        function: String,
        request: RpcRequestBuilder.() -> Unit = {}
    ): PostgrestResult

    /**
     * Executes a database function
     *
     * @param function The name of the function
     * @param parameters The parameters for the function
     * @param request Filter the result
     * @throws PostgrestRestException if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun rpc(
        function: String,
        parameters: JsonObject,
        request: RpcRequestBuilder.() -> Unit = {},
    ): PostgrestResult

    /**
     * Config for the Postgrest plugin
     * @param timeout Optional timeout for all requests. When set, requests will automatically abort after this duration to prevent indefinite hangs.
     * @param urlLengthLimit Maximum URL length in characters before warnings/errors are triggered. Defaults to 8000.
     * @param defaultSchema The default schema to use for the requests. Defaults to "public"
     * @param propertyConversionMethod The method to use to convert the property names to the column names in [PostgrestRequestBuilder] and [PostgrestUpdate]. Defaults to [PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE]
     * @param maxRetries Maximum number of retries for idempotent requests (GET, HEAD) that fail with transient errors (network errors, HTTP 503/520). Set to 0 to disable retries. Defaults to 3.
     */
    data class Config(
        var defaultSchema: String = "public",
        var propertyConversionMethod: PropertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE,
        override var requireValidSession: Boolean = false,
        var urlLengthLimit: Int = 8000,
        var timeout: Duration = 30.seconds,
        var maxRetries: Int = 3,
    ): MainConfig(), CustomSerializationConfig, AuthDependentPluginConfig {

        override var serializer: SupabaseSerializer? = null

    }

    companion object : SupabasePluginProvider<Config, Postgrest> {

        override val key = "rest"

        /**
         * The tag for the PostgREST logger.
         */
        const val LOGGING_TAG = "Supabase-PostgREST"

        /**
         * The current postgrest API version
         */
        const val API_VERSION = 1

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)
        override fun create(supabaseClient: SupabaseClient, config: Config): Postgrest {
            return PostgrestImpl(supabaseClient, config)
        }

    }

}


/**
 * With the postgrest plugin you can directly interact with your database via an api
 */
val SupabaseClient.postgrest: Postgrest
    get() = pluginManager.getPlugin(Postgrest)

/**
 * Creates a new [PostgrestQueryBuilder] for the given table
 * @param table The table to use for the requests
 */
fun SupabaseClient.from(table: String): PostgrestQueryBuilder = pluginManager.getPlugin(Postgrest).from(table)

/**
 * Creates a new [PostgrestQueryBuilder] for the given schema and table
 * @param schema The schema to use for the requests
 * @param table The table to use for the requests
 */
fun SupabaseClient.from(schema: String, table: String): PostgrestQueryBuilder = pluginManager.getPlugin(Postgrest).from(schema, table)