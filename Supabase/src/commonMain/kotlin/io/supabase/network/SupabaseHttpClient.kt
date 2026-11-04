@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")
package io.supabase.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

/**
 * The base HttpClients used by all main plugins
 */
abstract class SupabaseHttpClient {

    abstract suspend fun getDefaultHeaders(): Headers

    abstract suspend fun request(url: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse

    abstract suspend fun prepareRequest(url: String, builder: HttpRequestBuilder.() -> Unit): HttpStatement

    suspend inline fun get(url: String, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Get
        builder()
    }

    suspend inline fun post(url: String, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Post
        builder()
    }

    suspend inline fun <reified T> post(url: String, body: T, contentType: ContentType = ContentType.Any, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Post
        builder()
        contentType(contentType)
        setBody(body)
    }

    suspend inline fun <reified T> postJson(url: String, body: T, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = post(url, body, ContentType.Application.Json, builder)

    suspend inline fun delete(url: String, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Delete
        builder()
    }

    suspend inline fun <reified T> delete(url: String, body: T, contentType: ContentType = ContentType.Any, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Delete
        builder()
        contentType(contentType)
        setBody(body)
    }

    suspend inline fun <reified T> deleteJson(url: String, body: T, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = delete(url, body, ContentType.Application.Json, builder)

    suspend inline fun patch(url: String, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Patch
        builder()
    }

    suspend inline fun <reified T> patch(url: String, body: T, contentType: ContentType = ContentType.Any, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Patch
        builder()
        contentType(contentType)
        setBody(body)
    }

    suspend inline fun <reified T> patchJson(url: String, body: T, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = patch(url, body, ContentType.Application.Json, builder)

    suspend inline fun put(url: String, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Put
        builder()
    }

    suspend inline fun <reified T> put(url: String, body: T, contentType: ContentType = ContentType.Any, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = request(url) {
        method = HttpMethod.Put
        builder()
        contentType(contentType)
        setBody(body)
    }

    suspend inline fun <reified T> putJson(url: String, body: T, crossinline builder: HttpRequestBuilder.() -> Unit = {}) = put(url, body, ContentType.Application.Json, builder)

}