package io.supabase.auth.admin.custom.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/**
 * @param id Unique identifier (UUID)
 * @param providerType Provider type
 * @param identifier Provider identifier (e.g. `custom:mycompany`)
 * @param name Human-readable name
 * @param clientId OAuth client ID
 * @param acceptableClientIds Additional client IDs accepted during token validation
 * @param scopes OAuth scopes requested during authorization
 * @param pkceEnabled Whether PKCE is enabled
 * @param attributeMapping Mapping of provider attributes to Supabase user attributes
 * @param authorizationParams Additional parameters sent with the authorization request
 * @param enabled Whether the provider is enabled
 * @param emailOptional Whether email is optional for this provider
 * @param issuer OIDC issuer URL
 * @param discoveryUrl OIDC discovery URL
 * @param skipNonceCheck Whether to skip nonce check (OIDC)
 * @param authorizationUrl OAuth2 authorization URL
 * @param tokenUrl OAuth2 token URL
 * @param userinfoUrl OAuth2 userinfo URL
 * @param jwksUri JWKS URI for token verification
 * @param discoveryDocument OIDC discovery document (OIDC providers only)
 * @param createdAt Timestamp when the provider was created
 * @param updatedAt Timestamp when the provider was last updated
 * @param customClaimsAllowlist Allowlist of raw identity provider claim keys to copy verbatim into the
 *    user's `custom_claims` field (within `identity_data` and
 *    `raw_user_meta_data`), e.g. `["groups", "org_id", "mail"]`. This is an
 *    opt-in allowlist that defaults to empty (no claims captured) and operates
 *    independently from `attribute_mapping`.
 */
@Serializable
data class CustomOAuthProvider(
    val id: String,
    @SerialName("provider_type")
    val providerType: CustomProviderType,
    val identifier: String,
    val name: String,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("acceptable_client_ids")
    val acceptableClientIds: List<String>? = null,
    @SerialName("custom_claims_allowlist")
    val customClaimsAllowlist: List<String>? = null,
    val scopes: List<String>? = null,
    @SerialName("pkce_enabled")
    val pkceEnabled: Boolean? = null,
    @SerialName("attribute_mapping")
    val attributeMapping: JsonObject? = null,
    @SerialName("authorization_params")
    val authorizationParams: Map<String, String>? = null,
    val enabled: Boolean? = null,
    @SerialName("email_optional")
    val emailOptional: Boolean? = null,
    val issuer: String? = null,
    @SerialName("discovery_url")
    val discoveryUrl: String? = null,
    @SerialName("skip_nonce_check")
    val skipNonceCheck: Boolean? = null,
    @SerialName("authorization_url")
    val authorizationUrl: String? = null,
    @SerialName("token_url")
    val tokenUrl: String? = null,
    @SerialName("userinfo_url")
    val userinfoUrl: String? = null,
    @SerialName("jwks_uri")
    val jwksUri: String? = null,
    @SerialName("discovery_document")
    val discoveryDocument: OIDCDiscoveryDocument? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant
)