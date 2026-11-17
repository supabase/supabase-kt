package io.supabase.testing

import io.ktor.http.Url

fun Url.pathAfterVersion(): String {
    return encodedPath.substringAfter("/v1")
}