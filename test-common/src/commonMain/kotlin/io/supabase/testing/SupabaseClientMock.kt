package io.supabase.testing

import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.createSupabaseClient
import io.supabase.logging.LogLevel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

fun createMockedSupabaseClient(
    supabaseUrl: String = "https://projectref.supabase.co",
    supabaseKey: String = "project-anon-key",
    configuration: SupabaseClientBuilder.() -> Unit = {},
    requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("") },
): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey,
    ) {
        defaultLogLevel = LogLevel.DEBUG
        httpEngine = MockEngine {
            requestHandler(this, it)
        }
        configuration()
    }
}

suspend fun <T> withMockedSupabaseClient(
    supabaseUrl: String = "https://projectref.supabase.co",
    supabaseKey: String = "project-anon-key",
    configuration: SupabaseClientBuilder.() -> Unit = {},
    requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("") },
    clientHandler: suspend (SupabaseClient) -> T,
): T {
    val client = createMockedSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey,
        configuration = configuration,
        requestHandler = requestHandler
    )
    return try {
        clientHandler(client)
    } finally {
        client.close()
    }
}
