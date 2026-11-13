package io.supabase.auth.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth client details in an authorization request.
 * Only relevant when the OAuth 2.1 server is enabled in Supabase Auth.
 * @param id Unique identifier for the OAuth client (UUID)
 * @param name Human-readable name of the OAuth client
 * @param uri URI of the OAuth client's website
 * @param logoUri URI of the OAuth client's logo
 */
@Serializable
data class OAuthAuthorizationClient(
    val id: String,
    val name: String,
    val uri: String? = null,
    @SerialName("logo_uri") val logoUri: String? = null
)