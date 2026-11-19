import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.plugins.SupabasePlugin
import io.supabase.plugins.SupabasePluginProvider

class TestPlugin(override val config: Config, override val supabaseClient: SupabaseClient) :
    SupabasePlugin<TestPlugin.Config> {

    data class Config(var testValue: Boolean = false)

    companion object : SupabasePluginProvider<Config, TestPlugin> {

        override val key = "test"

        override fun createConfig(init: Config.() -> Unit): Config {
            return Config().apply(init)
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): TestPlugin {
            return TestPlugin(config, supabaseClient)
        }

        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            builder.useHTTPS = config.testValue
        }

    }

}