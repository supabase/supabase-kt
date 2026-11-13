package io.supabase.plugins

import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder

/**
 * A plugin provider is used to create a plugin instance. Typically inherited by a companion object of the plugin
 */
interface SupabasePluginProvider<Config, PluginInstance : SupabasePlugin<Config>> {

    /**
     * The key of this plugin. This key is used to identify the plugin within the [PluginManager]
     */
    val key: String

    /**
     * Create a config for this plugin using the [init] function
     */
    fun createConfig(init: Config.() -> Unit): Config

    /**
     * Change the [SupabaseClientBuilder] using the [config]
     */
    fun setup(builder: SupabaseClientBuilder, config: Config) {}

    /**
     * Create an instance of this plugin using the [supabaseClient] and [config]
     */
    fun create(supabaseClient: SupabaseClient, config: Config) : PluginInstance

}
