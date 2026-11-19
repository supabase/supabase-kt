package io.supabase.auth.providers.builtin

import io.supabase.annotations.SupabaseInternal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@SupabaseInternal
object CaptchaTokenSerializer: KSerializer<String> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CaptchaTokenSerializer") {
        element<JsonObject>("gotrue_meta_security")
    }

    override fun deserialize(decoder: Decoder): String {
        throw UnsupportedOperationException()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(buildJsonObject  {
            put("captcha_token", value)
        })
    }
}