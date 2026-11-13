package io.supabase.auth.providers.builtin

import io.supabase.auth.user.UserInfo
import io.supabase.exceptions.SupabaseEncodingException
import io.supabase.supabaseJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Authentication method with phone numbers and password
 */
data object Phone : DefaultAuthProvider<Phone.Config, UserInfo> {

    override val grantType: String = "password"

    /**
     * The configuration for the phone authentication method
     * @param phone The phone number of the user
     * @param password The password of the user
     * @param channel The channel to send the confirmation to
     * @param data Extra user metadata to include on signup
     */
    @Serializable
    data class Config(@SerialName("phone") var phone: String = "", var password: String = "", var channel: Channel = Channel.SMS, var data: JsonObject? = null): DefaultAuthProvider.Config()

    /**
     * Represents the phone number confirmation channel
     * @param value The short name of the channel
     */
    @Serializable(with = Channel.Companion::class)
    enum class Channel(val value: String) {
        /**
         * Send the confirmation via SMS
         */
        SMS("sms"),

        /**
         * Send the confirmation via WhatsApp. **Note:** WhatsApp is only supported by Twilio
         */
        WHATSAPP("whatsapp");

        companion object: KSerializer<Channel> {

            override val descriptor = PrimitiveSerialDescriptor("Channel", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): Channel {
                return entries.first { it.value == decoder.decodeString() }
            }

            override fun serialize(encoder: Encoder, value: Channel) {
                encoder.encodeString(value.value)
            }


        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeResult(json: JsonObject): UserInfo = try {
        supabaseJson.decodeFromJsonElement(json)
    } catch(e: MissingFieldException) {
        throw SupabaseEncodingException("Couldn't decode sign up phone result. Input: $json")
    }

    override fun encodeCredentials(credentials: Config.() -> Unit): JsonObject = supabaseJson.encodeToJsonElement(Config().apply(credentials)).jsonObject

}