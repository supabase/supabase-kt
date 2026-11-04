package io.supabase.auth.admin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A builder for updating a user.
 * @property email The user's email address
 * @property password The user's password
 * @property appMetadata Extra app metadata
 * @property userMetadata Extra user metadata
 * @property emailConfirm Automatically confirms the email address
 * @property phoneConfirm Automatically confirms the phone number
 * @property phone The user's phone number
 * @property role The `role` claim set in the user's access token JWT.
 *
 * When a user signs up, this role is set to `authenticated` by default. You should only modify the `role` if you need to provision several levels of admin access that have different permissions on individual columns in your database.
 *
 * Setting this role to `service_role` is not recommended as it grants the user admin privileges.
 * @property banDuration Determines how long a user is banned for.
 *  The format for the ban duration follows a strict sequence of decimal numbers with a unit suffix.
 *
 *  Valid time units are "ns", "us" (or "µs"), "ms", "s", "m", "h".
 *
 *  For example, some possible durations include: '300ms', '2h45m'.
 *
 *  Setting the ban duration to 'none' lifts the ban on the user.
 */
@Serializable
data class AdminUserUpdateBuilder(
    var email: String? = null,
    var password: String? = null,
    @SerialName("app_metadata")
    var appMetadata: JsonObject? = null,
    @SerialName("user_metadata")
    var userMetadata: JsonObject? = null,
    @SerialName("email_confirm")
    var emailConfirm: Boolean? = null,
    @SerialName("phone_confirm")
    var phoneConfirm: Boolean? = null,
    var phone: String? = null,
    @SerialName("ban_duration")
    var banDuration: String? = null,
    var role: String? = null
)
