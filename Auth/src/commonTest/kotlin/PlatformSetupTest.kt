import io.supabase.auth.Auth
import io.supabase.auth.auth
import io.supabase.auth.minimalConfig
import io.supabase.testing.createMockedSupabaseClient

internal suspend fun createMinimalAuthClient(
    autoSetup: Boolean,
    authHandler: suspend (Auth) -> Unit
) {
    val client = createMockedSupabaseClient(
        configuration = {
            install(Auth) {
                autoSetupPlatform = autoSetup
                minimalConfig()
            }
        }
    )
    try {
        authHandler(client.auth)
    } finally {
        client.close()
    }
}
