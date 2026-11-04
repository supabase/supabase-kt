package io.supabase.postgrest

import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.api.authenticatedSupabaseApi
import io.supabase.bodyOrNull
import io.supabase.exceptions.RestException
import io.supabase.logging.SupabaseLogger
import io.supabase.logging.createLogger
import io.supabase.postgrest.exception.PostgrestRestException
import io.supabase.postgrest.executor.RestRequestExecutor
import io.supabase.postgrest.query.PostgrestQueryBuilder
import io.supabase.postgrest.query.request.RpcRequestBuilder
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.client.plugins.timeout
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class PostgrestImpl(override val supabaseClient: SupabaseClient, override val config: Postgrest.Config) : Postgrest {

    override val logger: SupabaseLogger = supabaseClient.createLogger(Postgrest.LOGGING_TAG, config)
    override val apiVersion: Int
        get() = Postgrest.API_VERSION

    override val pluginKey: String
        get() = Postgrest.key

    override var serializer = config.serializer ?: supabaseClient.defaultSerializer

    @OptIn(SupabaseInternal::class)
    val api = supabaseClient.authenticatedSupabaseApi(this, urlLengthLimit = config.urlLengthLimit).withDefaultRequest {
        timeout {
            this.requestTimeoutMillis = config.timeout.inWholeMilliseconds
        }
    }

    override fun from(table: String): PostgrestQueryBuilder {
        return PostgrestQueryBuilder(
            postgrest = this,
            table = table,
        )
    }

    override fun from(schema: String, table: String): PostgrestQueryBuilder {
        return PostgrestQueryBuilder(
            postgrest = this,
            table = table,
            schema = schema,
        )
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        val body = supabaseClient.bodyOrNull<PostgrestErrorResponse>(response) ?: PostgrestErrorResponse("Unknown error")
        return PostgrestRestException(body.message, body.hint, body.details, body.code, response)
    }

    override suspend fun rpc(
        function: String,
        parameters: JsonObject,
        request: RpcRequestBuilder.() -> Unit
    ): PostgrestResult = rpcRequest(function, parameters, request)

    override suspend fun rpc(function: String, request: RpcRequestBuilder.() -> Unit): PostgrestResult = rpcRequest(function, null, request)

    private suspend fun rpcRequest(function: String, body: JsonObject? = null, request: RpcRequestBuilder.() -> Unit): PostgrestResult {
        val requestBuilder = RpcRequestBuilder(config.defaultSchema, config.propertyConversionMethod).apply {
            this.body = body
            request()
            if(method != RpcMethod.POST && body != null) {
                params.putAll(body.mapValues { listOf(it.value.toRpcParameter()) })
            }
        }
        return RestRequestExecutor.execute(postgrest = this, path = "rpc/$function", request = requestBuilder)
    }

    private fun JsonElement.toRpcParameter(): String = when (this) {
        is JsonArray -> joinToString(",", "{", "}") { it.toArrayElement() }
        is JsonPrimitive -> if (isString) content else toString()
        is JsonObject -> toString()
    }

    private fun JsonElement.toArrayElement(): String = when (this) {
        is JsonNull -> "NULL"
        is JsonArray -> toRpcParameter()
        is JsonPrimitive -> if (isString) quoteArrayElement(content) else content
        is JsonObject -> quoteArrayElement(toString())
    }

    private fun quoteArrayElement(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

}