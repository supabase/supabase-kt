import io.supabase.auth.Auth
import io.supabase.auth.status.SessionStatus

fun Auth.sessionSource() = (sessionStatus.value as SessionStatus.Authenticated).source

inline fun <reified T> Auth.sessionSourceAs() = sessionSource() as T