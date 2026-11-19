package io.supabase.auth.providers.builtin

import io.supabase.annotations.SupabaseInternal
import io.supabase.auth.providers.Apple
import io.supabase.auth.providers.Azure
import io.supabase.auth.providers.Facebook
import io.supabase.auth.providers.Google
import io.supabase.auth.providers.IDTokenProvider
import io.supabase.auth.user.UserInfo
import io.supabase.exceptions.SupabaseEncodingException
import io.supabase.supabaseJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Allows signing in with an OIDC ID token. The authentication provider used should be enabled and configured.
 *
 * Only [Apple], [Google], [Facebook] and [Azure] are supported as providers.
 *
 */
data object IDToken : DefaultAuthProvider<IDToken.Config, UserInfo> {

    override val grantType: String = "id_token"

    /**
     * The configuration for the id token authentication method
     * @param idToken OIDC ID token issued by the specified provider. The `iss` claim in the ID token must match the supplied provider. Some ID tokens contain an `at_hash` which require that you provide an `access_token` value to be accepted properly. If the token contains a `nonce` claim you must supply the nonce used to obtain the ID token.
     * @param provider The provider of the id token. Only [Apple], [Google], [Facebook] and [Azure] are supported
     * @param accessToken If the ID token contains an `at_hash` claim, then the hash of this value is compared to the value in the ID token.
     * @param nonce If the ID token contains a `nonce` claim, then the hash of this value is compared to the value in the ID token.
     */
    @Serializable
    data class Config(
        @SerialName("id_token") var idToken: String = "",
        var provider: IDTokenProvider? = null,
        @SerialName("access_token") var accessToken: String? = null,
        var nonce: String? = null,
        @property:SupabaseInternal @SerialName("link_identity") var linkIdentity: Boolean = false
    ) : DefaultAuthProvider.Config() {

        /**
         * This property is ignored by the ID token endpoint and has no effect.
         * It exists only for backward compatibility and will be removed in a future release.
         * Use [Email.Config.data] or [Phone.Config.data] for signup metadata instead.
         */
        @Deprecated("data is ignored by the ID token endpoint. It exists only for backward compatibility.", level = DeprecationLevel.WARNING)
        var data: JsonObject? = null
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeResult(json: JsonObject): UserInfo = try {
        supabaseJson.decodeFromJsonElement(json)
    } catch(e: MissingFieldException) {
        throw SupabaseEncodingException("Couldn't decode sign up id token result. Input: $json")
    }

    override fun encodeCredentials(credentials: Config.() -> Unit): JsonObject = supabaseJson.encodeToJsonElement(Config().apply(credentials)).jsonObject

}