package io.supabase.postgrest.query.request

import io.supabase.postgrest.Postgrest
import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.RpcMethod
import io.supabase.postgrest.query.PostgrestRequestBuilder
import io.ktor.http.HttpMethod

/**
 * Request builder for [Postgrest.rpc]
 */
class RpcRequestBuilder(defaultSchema: String, propertyConversionMethod: PropertyConversionMethod): PostgrestRequestBuilder(
    defaultSchema,
    propertyConversionMethod
) {

    /**
     * The HTTP method to use. Default is POST
     */
    var method: RpcMethod = RpcMethod.POST
        set(value) {
            httpMethod = value.httpMethod
            field = value
        }

    init {
        httpMethod = HttpMethod.Post
    }

}