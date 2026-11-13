package io.supabase

import io.supabase.annotations.SupabaseInternal
import io.ktor.http.URLBuilder
import io.ktor.http.Url

@SupabaseInternal
inline fun buildUrl(baseUrl: String, init: URLBuilder.() -> Unit): String {
    val builder = URLBuilder(baseUrl)
    builder.init()
    return builder.buildString()
}

@SupabaseInternal
inline fun buildUrl(baseUrl: Url, init: URLBuilder.() -> Unit): String {
    val builder = URLBuilder(baseUrl)
    builder.init()
    return builder.buildString()
}