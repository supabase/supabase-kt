package io.supabase.auth.user

import io.supabase.SupabaseSerializer
import io.supabase.auth.Auth
import io.supabase.encodeToJsonElement
import io.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * A builder for updating a user.
 * @param email The user's new email address
 * @param password The user's new password
 * @param phone The user's new phone number
 * @param nonce The nonce sent for reauthentication if the user's password is to be updated. Call [Auth.reauthenticate] to send the nonce to the user's email.
 * @param data Extra user metadata
 * @param currentPassword The user's current password
 * This is only ever present when the user is resetting their password and GOTRUE_SECURITY_UPDATE_PASSWORD_REQUIRE_CURRENT_PASSWORD is true.
 */
@Serializable
data class UserUpdateBuilder(
    var email: String? = null,
    var password: String? = null,
    @SerialName("phone")
    var phone: String? = null,
    var nonce: String? = null,
    var data: JsonObject? = null,
    var currentPassword: String? = null,
    @Transient @PublishedApi internal var serializer: SupabaseSerializer = KotlinXSerializer()
) {

    /**
     * Adds user-specific metadata
     */
    inline fun <reified T : Any> data(data: T) {
        this.data = serializer.encodeToJsonElement(data).jsonObject
    }

    /**
     * Adds user-specific metadata
     */
    inline fun data(builder: JsonObjectBuilder.() -> Unit) {
        data = buildJsonObject(builder)
    }

}
