package io.supabase.auth.admin.custom.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OIDC discovery document fields.
 * Populated when the server successfully fetches and validates the
 * provider's OpenID Connect discovery document.
 *
 * @param issuer The issuer identifier
 * @param authorizationEndpoint URL of the authorization endpoint
 * @param tokenEndpoint URL of the token endpoint
 * @param jwksUri URL of the JSON Web Key Set
 * @param userinfoEndpoint URL of the userinfo endpoint
 * @param revocationEndpoint URL of the revocation endpoint
 * @param supportedScopes List of supported scopes
 * @param supportedResponseTypes List of supported response types
 * @param supportedSubjectTypes List of supported subject types
 * @param supportedIdTokenSigningAlgs List of supported ID token signing algorithms
 */
@Serializable
data class OIDCDiscoveryDocument(
    val issuer: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("jwks_uri")
    val jwksUri: String,
    @SerialName("userinfo_endpoint")
    val userinfoEndpoint: String? = null,
    @SerialName("revocation_endpoint")
    val revocationEndpoint: String? = null,
    @SerialName("supported_scopes")
    val supportedScopes: List<String>? = null,
    @SerialName("supported_response_types")
    val supportedResponseTypes: List<String>? = null,
    @SerialName("supported_subject_types")
    val supportedSubjectTypes: List<String>? = null,
    @SerialName("supported_id_token_signing_algs")
    val supportedIdTokenSigningAlgs: List<String>? = null
)