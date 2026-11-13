package io.supabase.auth.admin.custom.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Parameters for creating a new custom provider.
 */
@Serializable
class CustomProviderBuilder {

    /**
     * Provider type
     */
    @SerialName("provider_type")
    var providerType: CustomProviderType? = null

    /**
     * Provider identifier (e.g. `custom:mycompany`)
     */
    var identifier: String? = null

    /**
     * Human-readable name
     */
    var name: String? = null

    /**
     * OAuth client ID
     */
    @SerialName("client_id")
    var clientId: String? = null

    /**
     * OAuth client secret (write-only, not returned in responses)
     */
    @SerialName("client_secret")
    var clientSecret: String? = null

    /**
     * Additional client IDs accepted during token validation
     */
    @SerialName("acceptable_client_ids")
    var acceptableClientIds: List<String>? = null

    /**
     * Allowlist of raw identity provider claim keys to copy verbatim into the
     * user's `custom_claims` field (within `identity_data` and
     * `raw_user_meta_data`), e.g. `["groups", "org_id", "mail"]`. This is an
     * opt-in allowlist that defaults to empty (no claims captured) and operates
     * independently from `attribute_mapping`.
     */
    @SerialName("custom_claims_allowlist")
    var customClaimsAllowlist: List<String>? = null

    /**
     * OAuth scopes requested during authorization
     */
    var scopes: List<String>? = null

    /**
     * Whether PKCE is enabled
     */
    @SerialName("pkce_enabled")
    var pkceEnabled: Boolean? = null

    /**
     * Mapping of provider attributes to Supabase user attributes
     */
    @SerialName("attribute_mapping")
    var attributeMapping: JsonObject? = null

    /**
     * Additional parameters sent with the authorization request
     */
    @SerialName("authorization_params")
    var authorizationParams: JsonObject? = null

    /**
     * Whether the provider is enabled
     */
    var enabled: Boolean? = null

    /**
     * Whether email is optional for this provider
     */
    @SerialName("email_optional")
    var emailOptional: Boolean? = null

    /**
     * OIDC issuer URL
     */
    var issuer: String? = null

    /**
     * OIDC discovery URL
     */
    @SerialName("discovery_url")
    var discoveryUrl: String? = null

    /**
     * Whether to skip nonce check (OIDC)
     */
    @SerialName("skip_nonce_check")
    var skipNonceCheck: Boolean? = null

    /**
     * OAuth2 authorization URL
     */
    @SerialName("authorization_url")
    var authorizationUrl: String? = null

    /**
     * OAuth2 token URL
     */
    @SerialName("token_url")
    var tokenUrl: String? = null

    /**
     * OAuth2 userinfo URL
     */
    @SerialName("userinfo_url")
    var userinfoUrl: String? = null

    /**
     * JWKS URI for token verification
     */
    @SerialName("jwks_uri")
    var jwksUri: String? = null

    internal fun checkRequired() {
        requireNotNull(identifier) {
            "Identifier must be set"
        }
        requireNotNull(name) {
            "Name must be set"
        }
        requireNotNull(clientId) {
            "Client id must be set"
        }
        requireNotNull(clientSecret) {
            "Client secret must be set"
        }
        when(requireNotNull(providerType) {
            "Provider type must be set"
        }) {
            CustomProviderType.OAUTH2 -> {
                requireNotNull(authorizationUrl) {
                    "Authorization url must be set"
                }
                requireNotNull(tokenUrl) {
                    "Token url must be set"
                }
                requireNotNull(userinfoUrl) {
                    "userInfoUrl must be set"
                }
            }
            CustomProviderType.OIDC -> {
                requireNotNull(issuer) {
                    "issuer must be set"
                }
            }
        }
    }

}