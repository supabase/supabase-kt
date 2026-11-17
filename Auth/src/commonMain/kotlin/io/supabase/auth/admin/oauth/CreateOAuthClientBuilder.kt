package io.supabase.auth.admin.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A builder for creating a new OAuth client.
 * @property clientName The name of the client
 * @property redirectUris The redirect URIs for the client
 * @property clientUri The URI of the client
 * @property grantTypes The grant types for the client
 * @property responseTypes The response types for the client
 * @property scope The scope for the client
 * @property tokenEndpointAuthMethod The token endpoint authentication method
 */
@Serializable
data class CreateOAuthClientBuilder(
    @SerialName("client_name")
    var clientName: String = "",
    @SerialName("redirect_uris")
    var redirectUris: List<String> = emptyList(),
    @SerialName("client_uri")
    var clientUri: String? = null,
    @SerialName("grant_types")
    var grantTypes: List<OAuthClientGrantType>? = null,
    @SerialName("response_types")
    var responseTypes: List<OAuthClientResponseType>? = null,
    var scope: String? = null,
    @SerialName("token_endpoint_auth_method")
    var tokenEndpointAuthMethod: OAuthClientTokenEndpointAuthMethod? = null
)
