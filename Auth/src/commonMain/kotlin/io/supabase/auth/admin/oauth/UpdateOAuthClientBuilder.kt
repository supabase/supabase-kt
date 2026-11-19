package io.supabase.auth.admin.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A builder for updating an existing OAuth client.
 * All fields are nullable for partial updates.
 * @property clientName The name of the client
 * @property clientUri The URI of the client
 * @property logoUri The logo URI of the client
 * @property redirectUris The redirect URIs for the client
 * @property grantTypes The grant types for the client
 * @property responseTypes The response types for the client
 * @property tokenEndpointAuthMethod The token endpoint authentication method
 */
@Serializable
data class UpdateOAuthClientBuilder(
    @SerialName("client_name")
    var clientName: String? = null,
    @SerialName("client_uri")
    var clientUri: String? = null,
    @SerialName("logo_uri")
    var logoUri: String? = null,
    @SerialName("redirect_uris")
    var redirectUris: List<String>? = null,
    @SerialName("grant_types")
    var grantTypes: List<OAuthClientGrantType>? = null,
    @SerialName("response_types")
    var responseTypes: List<OAuthClientResponseType>? = null,
    @SerialName("token_endpoint_auth_method")
    var tokenEndpointAuthMethod: OAuthClientTokenEndpointAuthMethod? = null
)
