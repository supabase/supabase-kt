package io.supabase.auth.exception

import io.supabase.auth.Auth

/**
 * An exception thrown when the signature of the JWT used in [Auth.getClaims] doesn't match a given key, resulting in the JWT being invalid
 * @see Auth.getClaims
 */
class InvalidJwtException: Exception("Invalid JWT signature")