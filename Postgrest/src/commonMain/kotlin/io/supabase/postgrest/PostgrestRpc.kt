@file:Suppress("MatchingDeclarationName")
package io.supabase.postgrest

import io.supabase.encodeToJsonElement
import io.supabase.exceptions.RestException
import io.supabase.postgrest.query.request.RpcRequestBuilder
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.jsonObject

/**
 * Enum class for the different HTTP methods that can be used in a RPC request
 * @property httpMethod The HTTP method
 */
enum class RpcMethod(val httpMethod: HttpMethod) {
    /**
     * HTTP HEAD method. If used, no body will be returned. Useful when using count. The parameters will be sent as query parameters.
     */
    HEAD(HttpMethod.Head),
    /**
     * HTTP POST method. Default method.
     */
    POST(HttpMethod.Post),
    /**
     * HTTP GET method. If used, the function will be called in read-only mode. The parameters will be sent as query parameters.
     */
    GET(HttpMethod.Get);
}

/**
 * Executes a database function
 *
 * @param function The name of the function
 * @param parameters The parameters for the function
 * @param request Filter the result
 * @throws RestException or one of its subclasses if the request failed
 */
suspend inline fun <reified T : Any> Postgrest.rpc(
    function: String,
    parameters: T,
    noinline request: RpcRequestBuilder.() -> Unit = {},
): PostgrestResult = rpc(function, serializer.encodeToJsonElement(parameters).jsonObject, request)