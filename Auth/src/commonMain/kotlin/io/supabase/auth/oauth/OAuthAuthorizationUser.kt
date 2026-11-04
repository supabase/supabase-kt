package io.supabase.auth.oauth

import kotlinx.serialization.Serializable

/**
 * Information about the [OAuthAuthorizationUser]
 * @param id User ID (UUID)
 * @param email User email
 */
@Serializable
data class OAuthAuthorizationUser(
    val id: String,
    val email: String
)
