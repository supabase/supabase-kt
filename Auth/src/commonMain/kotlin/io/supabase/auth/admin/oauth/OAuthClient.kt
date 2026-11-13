@file:Suppress("UndocumentedPublicProperty")

package io.supabase.auth.admin.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of an OAuth client.
 */
@Serializable
enum class OAuthClientType {
    @SerialName("public")
    PUBLIC,
    @SerialName("confidential")
    CONFIDENTIAL
}

/**
 * Represents a grant type for an OAuth client.
 */
@Serializable
enum class OAuthClientGrantType {
    @SerialName("authorization_code")
    AUTHORIZATION_CODE,
    @SerialName("refresh_token")
    REFRESH_TOKEN
}

/**
 * Represents a response type for an OAuth client.
 */
@Serializable
enum class OAuthClientResponseType {
    @SerialName("code")
    CODE
}

/**
 * Represents a registration type for an OAuth client.
 */
@Serializable
enum class OAuthClientRegistrationType {
    @SerialName("dynamic")
    DYNAMIC,
    @SerialName("manual")
    MANUAL
}

/**
 * Represents the token endpoint authentication method for an OAuth client.
 */
@Serializable
enum class OAuthClientTokenEndpointAuthMethod {
    @SerialName("none")
    NONE,
    @SerialName("client_secret_basic")
    CLIENT_SECRET_BASIC,
    @SerialName("client_secret_post")
    CLIENT_SECRET_POST
}

/**
 * Wrapper for the list clients response.
 */
@Serializable
internal data class OAuthClientListResponse(
    val clients: List<OAuthClient> = emptyList()
)

/**
 * Represents an OAuth 2.1 client registered with the Supabase Auth server.
 * @param clientId The unique identifier for the OAuth client
 * @param clientSecret The client secret (only returned on create/regenerate)
 * @param clientName The display name of the client
 * @param clientType The client type (public or confidential)
 * @param clientUri The URI of the client's homepage
 * @param logoUri The URI of the client's logo
 * @param redirectUris The allowed redirect URIs
 * @param grantTypes The allowed grant types
 * @param responseTypes The allowed response types
 * @param scope The allowed scope
 * @param tokenEndpointAuthMethod The token endpoint authentication method
 * @param registrationType The registration type (dynamic or manual)
 * @param createdAt The creation timestamp
 * @param updatedAt The last update timestamp
 */
@Serializable
data class OAuthClient(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String? = null,
    @SerialName("client_name")
    val clientName: String,
    @SerialName("client_type")
    val clientType: OAuthClientType? = null,
    @SerialName("client_uri")
    val clientUri: String? = null,
    @SerialName("logo_uri")
    val logoUri: String? = null,
    @SerialName("redirect_uris")
    val redirectUris: List<String> = emptyList(),
    @SerialName("grant_types")
    val grantTypes: List<OAuthClientGrantType> = emptyList(),
    @SerialName("response_types")
    val responseTypes: List<OAuthClientResponseType> = emptyList(),
    val scope: String? = null,
    @SerialName("token_endpoint_auth_method")
    val tokenEndpointAuthMethod: OAuthClientTokenEndpointAuthMethod? = null,
    @SerialName("registration_type")
    val registrationType: OAuthClientRegistrationType? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
