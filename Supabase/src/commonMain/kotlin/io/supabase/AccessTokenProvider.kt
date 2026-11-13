package io.supabase

/**
 * Optional function for using a third-party authentication system with
 * Supabase. The function should return an access token or ID token (JWT) by
 * obtaining it from the third-party auth client library. Note that this
 * function may be called concurrently and many times. Use memoization and
 * locking techniques if this is not supported by the client libraries.
 */
typealias AccessTokenProvider = suspend () -> String?