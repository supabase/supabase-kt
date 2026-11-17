@file:Suppress("UndocumentedPublicClass")
package io.supabase.auth.providers

data object Google : IDTokenProvider() {

    override val name = "google"

}

data object Discord : OAuthProvider() {

    override val name = "discord"

}

data object Github : OAuthProvider() {

    override val name = "github"

}

data object Gitlab : OAuthProvider() {

    override val name = "gitlab"

}

data object Keycloak : OAuthProvider() {

    override val name = "keycloak"

}

data object LinkedIn : OAuthProvider() {

    override val name = "linkedin"

}

data object LinkedInOIDC : OAuthProvider() {

    override val name = "linkedin_oidc"

}

data object Notion : OAuthProvider() {

    override val name = "notion"

}

data object Slack : OAuthProvider() {

    override val name = "slack"

}

data object SlackOIDC : OAuthProvider() {

    override val name = "slack_oidc"

}

data object Twitch : OAuthProvider() {

    override val name = "twitch"

}

/**
 * OAuth 1.0a
 */
data object Twitter : OAuthProvider() {

    override val name = "twitter"

}

/**
 * OAuth 2.0
 */
data object X : OAuthProvider() {

    override val name = "x"

}

data object WorkOS : OAuthProvider() {

    override val name = "workos"

}

data object Zoom : OAuthProvider() {

    override val name = "zoom"

}

data object Bitbucket : OAuthProvider() {

    override val name = "bitbucket"

}

data object Azure : IDTokenProvider() {

    override val name = "azure"

}

data object Apple : IDTokenProvider() {

    override val name = "apple"

}

data object Spotify : OAuthProvider() {

    override val name = "spotify"

}

data object Kakao : IDTokenProvider() {

    override val name = "kakao"

}

data object Facebook : IDTokenProvider() {

    override val name = "facebook"
  
}

data object Figma : OAuthProvider() {

    override val name = "figma"

}

data object Fly : OAuthProvider() {

    override val name = "fly"

}

/**
 * Creates a new [OAuthProvider] with the given name
 */
operator fun OAuthProvider.Companion.invoke(provider: String) = object : OAuthProvider() {

    override val name = provider

}