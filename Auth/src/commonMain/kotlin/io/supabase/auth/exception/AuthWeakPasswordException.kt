package io.supabase.auth.exception

import io.ktor.client.statement.HttpResponse

/**
 * Exception thrown on sign-up if the password is too weak
 * @param description The description of the exception.
 * @param reasons The reasons why the password is weak.
 */
class AuthWeakPasswordException(
    description: String,
    response: HttpResponse,
    val reasons: List<String>
) : AuthRestException(
    CODE,
    description,
    response
) {

    internal companion object {
        const val CODE = "weak_password"
    }

}
