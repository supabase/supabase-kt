package io.supabase.auth.providers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents an OAuthProvider capable of authenticating via an id token
 */
@Serializable(with = IDTokenProvider.Companion::class)
sealed class IDTokenProvider: OAuthProvider() {

    companion object : KSerializer<IDTokenProvider> {
        override val descriptor = PrimitiveSerialDescriptor("IDTokenProvider", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): IDTokenProvider {
            throw UnsupportedOperationException()
        }

        override fun serialize(encoder: Encoder, value: IDTokenProvider) {
            encoder.encodeString(value.name)
        }

    }

}