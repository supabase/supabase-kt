package io.supabase.auth.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * An OAuth grant representing a user's authorization of an OAuth client.
 * Only relevant when the OAuth 2.1 server is enabled in Supabase Auth.
 * @param client OAuth client information
 * @param scopes Array of scopes granted to this client
 * @param grantedAt Timestamp when the grant was created (ISO 8601 date-time)
 */
@Serializable
data class OAuthGrant(
    val client: OAuthAuthorizationClient,
    val scopes: List<String>,
    @SerialName("granted_at") val grantedAt: Instant
)
