package io.supabase.auth.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth authorization details when user needs to provide consent.
 * Only relevant when the OAuth 2.1 server is enabled in Supabase Auth.
 *
 * This response includes all information needed to display a consent page:
 * client details, user info, requested scopes, and where the user will be redirected.
 *
 * Note: [redirectUri] is the base URI (e.g., "https://app.com/callback") without
 * query parameters. After consent, you'll receive a complete redirect url with
 * the authorization code and state parameters appended.
 * @param authorizationId The authorization ID used to approve or deny the request
 * @param redirectUri The OAuth client's registered redirect URI (base URI without query parameters)
 * @param client OAuth client requesting authorization
 * @param user User object associated with the authorization
 * @param scope Space-separated list of requested scopes (e.g., "openid profile email")
 */
@Serializable
data class OAuthAuthorizationDetails(
    @SerialName("authorization_id") val authorizationId: String,
    @SerialName("redirect_uri") val redirectUri: String,
    val client: OAuthAuthorizationClient,
    val user: OAuthAuthorizationUser,
    val scope: String
): OAuthAuthorizationDetailsResponse