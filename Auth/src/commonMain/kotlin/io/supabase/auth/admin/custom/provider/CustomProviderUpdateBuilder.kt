package io.supabase.auth.admin.custom.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Parameters for updating an existing custom provider.
 * All fields are optional. Only provided fields will be updated.
 * @param name Human-readable name
 * @param clientId OAuth client ID
 * @param clientSecret OAuth client secret (write-only, not returned in responses)
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
 * @param customClaimsAllowlist Allowlist of raw identity provider claim keys to copy verbatim into the
 *    user's `custom_claims` field (within `identity_data` and
 *    `raw_user_meta_data`), e.g. `["groups", "org_id", "mail"]`. This is an
 *    opt-in allowlist that defaults to empty (no claims captured) and operates
 *    independently from `attribute_mapping`.
 */
@Serializable
data class CustomProviderUpdateBuilder(
    var name: String? = null,
    @SerialName("client_id")
    var clientId: String? = null,
    @SerialName("client_secret")
    var clientSecret: String? = null,
    @SerialName("acceptable_client_ids")
    var acceptableClientIds: List<String>? = null,
    var scopes: List<String>? = null,
    @SerialName("pkce_enabled")
    var pkceEnabled: Boolean? = null,
    @SerialName("attribute_mapping")
    var attributeMapping: JsonObject? = null,
    @SerialName("authorization_params")
    var authorizationParams: JsonObject? = null,
    var enabled: Boolean? = null,
    @SerialName("email_optional")
    var emailOptional: Boolean? = null,
    var issuer: String? = null,
    @SerialName("discovery_url")
    var discoveryUrl: String? = null,
    @SerialName("skip_nonce_check")
    var skipNonceCheck: Boolean? = null,
    @SerialName("authorization_url")
    var authorizationUrl: String? = null,
    @SerialName("token_url")
    var tokenUrl: String? = null,
    @SerialName("userinfo_url")
    var userinfoUrl: String? = null,
    @SerialName("jwks_uri")
    var jwksUri: String? = null,
    @SerialName("custom_claims_allowlist")
    val customClaimsAllowlist: List<String>? = null
)