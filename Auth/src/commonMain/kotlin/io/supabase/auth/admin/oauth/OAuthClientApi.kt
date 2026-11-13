package io.supabase.auth.admin.oauth

import io.supabase.auth.api.AuthenticatedSupabaseApi
import io.supabase.safeBody

/**
 * API for managing OAuth 2.1 clients via the admin interface.
 * Service role access token is required.
 */
interface OAuthClientApi {

    /**
     * Lists all OAuth clients.
     * @param page The page number for pagination
     * @param perPage The number of clients per page
     * @return A list of OAuth clients
     */
    suspend fun listClients(page: Int? = null, perPage: Int? = null): List<OAuthClient>

    /**
     * Creates a new OAuth client.
     * @param builder The builder for configuring the new client
     * @return The newly created OAuth client
     */
    suspend fun createClient(builder: CreateOAuthClientBuilder.() -> Unit): OAuthClient

    /**
     * Retrieves an OAuth client by its client ID.
     * @param clientId The client ID
     * @return The OAuth client
     */
    suspend fun getClient(clientId: String): OAuthClient

    /**
     * Updates an existing OAuth client.
     * @param clientId The client ID
     * @param builder The builder for configuring the update
     * @return The updated OAuth client
     */
    suspend fun updateClient(clientId: String, builder: UpdateOAuthClientBuilder.() -> Unit): OAuthClient

    /**
     * Deletes an OAuth client.
     * @param clientId The client ID
     */
    suspend fun deleteClient(clientId: String)

    /**
     * Regenerates the client secret for an OAuth client.
     * @param clientId The client ID
     * @return The OAuth client with the new secret
     */
    suspend fun regenerateClientSecret(clientId: String): OAuthClient

}

@PublishedApi
internal class OAuthClientApiImpl(val api: AuthenticatedSupabaseApi) : OAuthClientApi {

    override suspend fun listClients(page: Int?, perPage: Int?): List<OAuthClient> {
        return api.get("admin/oauth/clients") {
            page?.let { url.parameters.append("page", it.toString()) }
            perPage?.let { url.parameters.append("per_page", it.toString()) }
        }.safeBody<OAuthClientListResponse>().clients
    }

    override suspend fun createClient(builder: CreateOAuthClientBuilder.() -> Unit): OAuthClient {
        val createBuilder = CreateOAuthClientBuilder().apply(builder)
        return api.postJson("admin/oauth/clients", createBuilder).safeBody()
    }

    override suspend fun getClient(clientId: String): OAuthClient {
        return api.get("admin/oauth/clients/$clientId").safeBody()
    }

    override suspend fun updateClient(clientId: String, builder: UpdateOAuthClientBuilder.() -> Unit): OAuthClient {
        val updateBuilder = UpdateOAuthClientBuilder().apply(builder)
        return api.putJson("admin/oauth/clients/$clientId", updateBuilder).safeBody()
    }

    override suspend fun deleteClient(clientId: String) {
        api.delete("admin/oauth/clients/$clientId")
    }

    override suspend fun regenerateClientSecret(clientId: String): OAuthClient {
        return api.post("admin/oauth/clients/$clientId/regenerate_secret").safeBody()
    }

}
