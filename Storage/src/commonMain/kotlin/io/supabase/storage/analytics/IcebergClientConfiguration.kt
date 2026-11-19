package io.supabase.storage.analytics

import io.ktor.http.Headers

/**
 * Configuration for a custom iceberg client e.g. for Java Apache Iceberg
 * @param baseUrl The REST url
 * @param defaultHeaders Default headers: apikey, auth and miscellaneous Supabase headers.
 */
data class IcebergClientConfiguration(
    val baseUrl: String,
    val defaultHeaders: suspend () -> Headers
)