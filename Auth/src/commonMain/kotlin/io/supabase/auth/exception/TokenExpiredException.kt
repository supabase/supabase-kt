package io.supabase.auth.exception

/**
 * Exception thrown when trying to make a request with an expired access token and the force-refresh failed.
 */
class TokenExpiredException: Exception("The token has expired and a force-refresh was unsuccessful")