import io.supabase.auth.AuthConfig

actual fun AuthConfig.platformSettings() {
    enableLifecycleCallbacks = false
}