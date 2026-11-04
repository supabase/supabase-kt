package io.supabase.auth.providers.builtin

import io.supabase.auth.user.UserInfo
import io.supabase.exceptions.SupabaseEncodingException
import io.supabase.supabaseJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Authentication method with email and password
 */
data object Email : DefaultAuthProvider<Email.Config, UserInfo> {

    override val grantType: String = "password"

    /**
     * The configuration for the email authentication method
     * @param email The email of the user
     * @param password The password of the user
     * @param data Extra user metadata to include on signup
     */
    @Serializable
    data class Config(var email: String = "", var password: String = "", var data: JsonObject? = null): DefaultAuthProvider.Config()

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeResult(json: JsonObject): UserInfo = try {
        supabaseJson.decodeFromJsonElement(json)
    } catch(e: MissingFieldException) {
        throw SupabaseEncodingException("Couldn't decode sign up email result. Input: $json")
    }

    override fun encodeCredentials(credentials: Config.() -> Unit): JsonObject = supabaseJson.encodeToJsonElement(Config().apply(credentials)).jsonObject

}