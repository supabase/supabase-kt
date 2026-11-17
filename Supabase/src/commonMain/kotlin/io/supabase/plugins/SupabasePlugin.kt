package io.supabase.plugins

import io.supabase.SupabaseClient

/**
 * A plugin is a feature that can be installed into the supabase client
 */
interface SupabasePlugin<Config> {

    /**
     * The config for this plugin
     */
    val config: Config

    /**
     * The corresponding [SupabaseClient]
     */
    val supabaseClient: SupabaseClient

    /**
     * Free all resources used by this plugin
     */
    suspend fun close() {}

    /**
     * Initialize the plugin. Use this function, if you want to execute code after the plugin has been created but also have to access other plugins.
     *
     * **Note:** This function is called by the [SupabaseClient] after all plugins have been created. Do not call this function manually.
     */
    fun init() {}

}