package io.supabase.auth.admin

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Link types used in [AdminApi.generateLinkFor]
 */
sealed interface LinkType<C: LinkType.Config> {

    /**
     * The type of the link
     */
    val type: String

    @SupabaseInternal
    fun createConfig(config: C.() -> Unit): C

    /**
     * The default configuration for a [LinkType]
     */
    @Serializable
    open class Config {

        /**
         * The email of the user
         */
        var email: String = ""

    }

    /**
     * The signup link type
     */
    data object Signup: LinkType<Signup.Config> {

        override val type = "signup"

        /**
         * The configuration for the [Signup] link type
         * @param password The password for the new user
         * @param data Custom data for the new user
         */
        @Serializable
        data class Config(
            var password: String = "",
            var data: JsonObject? = null
        ): LinkType.Config()

        @SupabaseInternal
        override fun createConfig(config: Config.() -> Unit): Config = Config().apply(config)

    }

    /**
     * The invite link type
     */
    data object Invite : LinkType<Config> {

        override val type = "invite"

        @SupabaseInternal
        override fun createConfig(config: Config.() -> Unit): Config = Config().apply(config)

    }

    /**
     * The magic link type
     */
    data object MagicLink : LinkType<Config> {

        override val type: String = "magiclink"

        @SupabaseInternal
        override fun createConfig(config: Config.() -> Unit): Config = Config().apply(config)

    }

    /**
     * The recovery link type
     */
    data object RecoveryLink : LinkType<Config> {

        override val type: String = "recovery"

        @SupabaseInternal
        override fun createConfig(config: Config.() -> Unit): Config = Config().apply(config)

    }

    /**
     * The email change link type
     */
    data object EmailChangeCurrent : LinkType<EmailChangeCurrent.Config> {

        override val type: String = "email_change_current"

        /**
         * The configuration for the [EmailChangeCurrent] and [EmailChangeNew] link type
         * @param newEmail The new email of the user
         */
        @Serializable
        data class Config(
            @SerialName("new_email")
            var newEmail: String = ""
        ): LinkType.Config()

        @SupabaseInternal
        override fun createConfig(config: Config.() -> Unit): Config = Config().apply(config)

    }

    /**
     * The email change link type
     */
    data object EmailChangeNew : LinkType<EmailChangeCurrent.Config> {

        override val type: String = "email_change_new"

        @SupabaseInternal
        override fun createConfig(config: EmailChangeCurrent.Config.() -> Unit): EmailChangeCurrent.Config = EmailChangeCurrent.Config().apply(config)

    }

}