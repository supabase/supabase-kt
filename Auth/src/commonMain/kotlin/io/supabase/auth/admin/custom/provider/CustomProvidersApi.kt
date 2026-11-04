package io.supabase.auth.admin.custom.provider

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.safeBody
import io.supabase.supabaseJson
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Contains all custom OIDC/OAuth provider administration methods.
 */
interface CustomProvidersApi {

    /**
     * Lists all custom providers with optional type filter.
     *
     * This function should only be called on a server. Never expose your `service_role` key in the browser.
     * @param type The custom provider type
     */
    suspend fun listProviders(type: CustomProviderType? = null): List<CustomOAuthProvider>

    /**
     * Creates a new custom OIDC/OAuth provider.
     *
     * For OIDC providers, the server fetches and validates the OpenID Connect discovery document
     * from the issuer's well-known endpoint (or the provided `discovery_url`) at creation time.
     * This may return a validation error (`error_code: "validation_failed"`) if the discovery
     * document is unreachable, not valid JSON, missing required fields, or if the issuer
     * in the document does not match the expected issuer.
     *
     * This function should only be called on a server. Never expose your `service_role` key in the browser.
     * @param builder Builder for the new provider
     */
    suspend fun createProvider(builder: CustomProviderBuilder.() -> Unit): CustomOAuthProvider

    /**
     * Gets details of a specific custom provider by identifier.
     *
     * This function should only be called on a server. Never expose your `service_role` key in the browser.
     * @param identifier The identifier of the OAuth provider
     */
    suspend fun getProvider(identifier: String): CustomOAuthProvider

    /**
     * Updates an existing custom provider.
     *
     * When `issuer` or `discovery_url` is changed on an OIDC provider, the server re-fetches and
     * validates the discovery document before persisting. This may return a validation error
     * (`error_code: "validation_failed"`) if the discovery document is unreachable, invalid, or
     * the issuer does not match.
     *
     * This function should only be called on a server. Never expose your `service_role` key in the browser.
     * @param identifier The identifier of the OAuth provider
     * @param builder The update builder
     */
    suspend fun updateProvider(identifier: String, builder: CustomProviderUpdateBuilder.() -> Unit): CustomOAuthProvider

    /**
     * Deletes a custom provider.
     *
     * This function should only be called on a server. Never expose your `service_role` key in the browser.
     * @param identifier The identifier of the OAuth provider
     */
    suspend fun deleteProvider(identifier: String)

}

internal class CustomProvidersApiImpl(
    private val api: AuthenticatedSupabaseApi
): CustomProvidersApi {

    override suspend fun listProviders(type: CustomProviderType?): List<CustomOAuthProvider> {
        val response = api.get("") {
            type?.let {
                parameter("type", it.name.lowercase())
            }
        }.safeBody<JsonObject>()
        return response["providers"]?.let { supabaseJson.decodeFromJsonElement(it) } ?: emptyList()
    }

    override suspend fun createProvider(builder: CustomProviderBuilder.() -> Unit): CustomOAuthProvider {
        val builder = CustomProviderBuilder().apply(builder)
        builder.checkRequired()
        return api.postJson("", builder).safeBody()
    }

    override suspend fun getProvider(identifier: String): CustomOAuthProvider {
        return api.get(identifier).safeBody()
    }

    override suspend fun updateProvider(
        identifier: String,
        builder: CustomProviderUpdateBuilder.() -> Unit
    ): CustomOAuthProvider {
        val builder = CustomProviderUpdateBuilder().apply(builder)
        return api.putJson(identifier, builder).safeBody()
    }

    override suspend fun deleteProvider(identifier: String) {
        api.delete(identifier)
    }
}