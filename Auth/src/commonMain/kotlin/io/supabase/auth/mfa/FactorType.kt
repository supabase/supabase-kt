package io.supabase.auth.mfa

import io.supabase.annotations.SupabaseInternal
import io.supabase.supabaseJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents an MFA factor type
 * @param value The name of the factor type
 */
sealed class FactorType<Config, Response>(val value: String) {

    @SupabaseInternal
    abstract suspend fun encodeConfig(config: Config.() -> Unit): JsonObject

    @SupabaseInternal
    abstract suspend fun decodeResponse(json: JsonObject): Response

    /**
     * TOTP (timed one-time password) MFA factor
     */
    data object TOTP : FactorType<TOTP.Config, TOTP.Response>("totp") {

        /**
         * @param secret The secret used to generate the TOTP code
         * @param qrCode The QR code used to enroll the TOTP factor in a svg format
         * @param uri The URI used to enroll the TOTP factor
         */
        @Serializable
        data class Response(
            val secret: String,
            @SerialName("qr_code")
            val qrCode: String,
            val uri: String
        )

        /**
         * @param issuer Domain which the user is enrolling with
         */
        @Serializable
        data class Config(
            var issuer: String? = null,
        )

        override suspend fun decodeResponse(json: JsonObject): Response {
            return supabaseJson.decodeFromJsonElement(json["totp"]?.jsonObject ?: error("No 'totp' object found in factor response"))
        }

        override suspend fun encodeConfig(config: Config.() -> Unit): JsonObject {
            return supabaseJson.encodeToJsonElement(Config().apply(config)).jsonObject
        }

    }

    /**
     * Phone MFA factor
     */
    data object Phone : FactorType<Phone.Config, Phone.Response>("phone") {

        /**
         * @param phone Phone number of the MFA factor in E.164 format. Used to send messages
         */
        @Serializable
        data class Response(
            val phone: String
        )

        /**
         * @param phone The phone number to send the SMS to. Number should conform to E.164 format
         */
        @Serializable
        data class Config(
            var phone: String? = null,
        )

        override suspend fun decodeResponse(json: JsonObject): Response {
            return Response(json["phone"]?.jsonPrimitive?.contentOrNull ?: error("No 'phone' entry found in factor response"))
        }

        override suspend fun encodeConfig(config: Config.() -> Unit): JsonObject {
            return supabaseJson.encodeToJsonElement(Config().apply(config)).jsonObject
        }

    }

}