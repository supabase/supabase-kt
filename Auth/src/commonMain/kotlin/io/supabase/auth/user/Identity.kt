@file:Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction", "UndocumentedPublicProperty")
package io.supabase.auth.user


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Identity(
    @SerialName("id")
    val id: String,
    @SerialName("identity_data")
    val identityData: JsonObject,
    @SerialName("identity_id")
    val identityId: String? = null,
    @SerialName("last_sign_in_at")
    val lastSignInAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("provider")
    val provider: String,
    @SerialName("user_id")
    val userId: String
)