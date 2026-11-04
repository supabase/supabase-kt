package io.supabase.auth.admin.custom.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type of custom identity provider.
 */
@Serializable
enum class CustomProviderType {
    /**
     * OAuth2 custom provider type
     */
    @SerialName("oauth2") OAUTH2,

    /**
     * OIDC custom provider type
     */
    @SerialName("oidc") OIDC
}