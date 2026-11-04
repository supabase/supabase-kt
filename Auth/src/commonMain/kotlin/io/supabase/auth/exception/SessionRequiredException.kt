package io.supabase.auth.exception

import io.supabase.StringMasking
import io.ktor.http.Url

/**
 * An exception thrown when trying to perform a request that requires a valid session while no user is logged in.
 * @param url The request url
 */
class SessionRequiredException(url: String): Exception("You need to be logged in to perform this request\nURL: ${StringMasking.maskUrl(
    Url(url)
)}")
