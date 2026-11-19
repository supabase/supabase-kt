package io.supabase.auth.oauth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Response type for getting OAuth authorization details.
 * Possible subclasses: full [authorization details][OAuthAuthorizationDetails] (if consent needed) or [redirect URL][OAuthRedirect] (if already consented).
 * Only relevant when the OAuth 2.1 server is enabled in Supabase Auth.
 */
@Serializable(with = OAuthAuthorizationDetailsResponse.Companion::class)
sealed interface OAuthAuthorizationDetailsResponse {

    companion object : JsonContentPolymorphicSerializer<OAuthAuthorizationDetailsResponse>(OAuthAuthorizationDetailsResponse::class) {
        override fun selectDeserializer(element: JsonElement) =
            when {
                "redirect_uri" in element.jsonObject -> OAuthAuthorizationDetails.serializer()
                "redirect_url" in element.jsonObject -> OAuthRedirect.serializer()
                else -> error("Unknown response body")
            }
    }

}