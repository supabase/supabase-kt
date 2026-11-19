package io.supabase.testing

import io.supabase.network.SupabaseHttpClient
import io.supabase.supabaseJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.Headers
import io.ktor.serialization.kotlinx.json.json

class MockedHttpClient(
    private val defaultHeaders: Headers = Headers.Empty,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("") },
): SupabaseHttpClient() {

    override suspend fun getDefaultHeaders(): Headers {
        return defaultHeaders
    }

    private val httpClient = HttpClient(MockEngine {
        handler(this, it)
    }) {
        install(ContentNegotiation) {
            json(supabaseJson)
        }
    }

    override suspend fun request(
        url: String,
        builder: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        return httpClient.request(url) {
            builder()
        }
    }

    override suspend fun prepareRequest(
        url: String,
        builder: HttpRequestBuilder.() -> Unit
    ): HttpStatement {
        return httpClient.prepareRequest(url) { builder() }
    }

}