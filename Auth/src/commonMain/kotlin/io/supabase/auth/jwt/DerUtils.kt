@file:Suppress("MagicNumber")
package io.supabase.auth.jwt

internal fun rsaJwkToDer(jwk: JWK): ByteArray {
    val n = JWTUtils.decodeBase64ByteArray(jwk.getParam<String>("n"))
    val e = JWTUtils.decodeBase64ByteArray(jwk.getParam<String>("e"))

    val rsaPublicKey = buildAsn1Sequence {
        addInteger(n)
        addInteger(e)
    }

    val algorithmIdentifier = buildAsn1Sequence {
        addOid(RSA_OID)
        addNull()
    }

    return buildAsn1Sequence {
        addRawBytes(algorithmIdentifier)
        addBitString(rsaPublicKey)
    }
}

internal fun ecJwkToDer(jwk: JWK): ByteArray {
    val x = JWTUtils.decodeBase64ByteArray(jwk.getParam<String>("x"))
    val y = JWTUtils.decodeBase64ByteArray(jwk.getParam<String>("y"))

    val publicKeyBytes = byteArrayOf(0x04) + x + y

    val algorithmIdentifier = buildAsn1Sequence {
        addOid(EC_OID)
        addOid(P256_OID)
    }

    return buildAsn1Sequence {
        addRawBytes(algorithmIdentifier)
        addBitString(publicKeyBytes)
    }
}

internal fun ecdsaRawToDer(rawSignature: ByteArray): ByteArray {
    val componentLength = rawSignature.size / 2
    val r = rawSignature.copyOfRange(0, componentLength)
    val s = rawSignature.copyOfRange(componentLength, rawSignature.size)

    return buildAsn1Sequence {
        addInteger(r)
        addInteger(s)
    }
}

private val RSA_OID = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)
private val EC_OID = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01)
private val P256_OID = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)

private inline fun buildAsn1Sequence(block: Asn1Builder.() -> Unit): ByteArray {
    return Asn1Builder().apply(block).toSequence()
}

private class Asn1Builder {
    private val content = mutableListOf<Byte>()

    fun addInteger(value: ByteArray) {
        var start = 0
        while (start < value.size - 1 && value[start].toInt() == 0 && value[start + 1].toInt() >= 0) {
            start++
        }

        val trimmed = value.copyOfRange(start, value.size)
        val needsPadding = trimmed.isNotEmpty() && trimmed[0].toInt() < 0

        content.add(0x02) // Tag
        if (needsPadding) {
            addLength(trimmed.size + 1)
            content.add(0x00)
        } else {
            addLength(trimmed.size)
        }
        content.addAll(trimmed.toList())
    }

    fun addOid(oid: ByteArray) {
        content.add(0x06)
        addLength(oid.size)
        content.addAll(oid.toList())
    }

    fun addNull() {
        content.add(0x05)
        content.add(0x00)
    }

    fun addBitString(data: ByteArray) {
        content.add(0x03)
        addLength(data.size + 1)
        content.add(0x00)
        content.addAll(data.toList())
    }

    fun addRawBytes(data: ByteArray) {
        content.addAll(data.toList())
    }

    private fun addLength(length: Int) {
        when {
            length < 128 -> content.add(length.toByte())
            length < 256 -> {
                content.add(0x81.toByte())
                content.add(length.toByte())
            }
            else -> {
                content.add(0x82.toByte())
                content.add((length shr 8).toByte())
                content.add((length and 0xFF).toByte())
            }
        }
    }

    fun toSequence(): ByteArray {
        val result = mutableListOf<Byte>()
        result.add(0x30)
        val contentBytes = content.toByteArray()
        when {
            contentBytes.size < 128 -> result.add(contentBytes.size.toByte())
            contentBytes.size < 256 -> {
                result.add(0x81.toByte())
                result.add(contentBytes.size.toByte())
            }
            else -> {
                result.add(0x82.toByte())
                result.add((contentBytes.size shr 8).toByte())
                result.add((contentBytes.size and 0xFF).toByte())
            }
        }
        result.addAll(contentBytes.toList())
        return result.toByteArray()
    }
}
