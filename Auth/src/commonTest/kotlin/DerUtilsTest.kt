import io.supabase.auth.jwt.JWK
import io.supabase.auth.jwt.ecJwkToDer
import io.supabase.auth.jwt.ecdsaRawToDer
import io.supabase.auth.jwt.rsaJwkToDer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DerUtilsTest {

    @Test
    fun testRsaJwkToDer() {
        val rsaJwkJson = """
            {
                "kty": "RSA",
                "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                "e": "AQAB",
                "alg": "RS256",
                "kid": "test-key-id",
                "key_ops": ["verify"]
            }
        """.trimIndent()

        val jwk = JWK(Json.decodeFromString<JsonObject>(rsaJwkJson))
        val der = rsaJwkToDer(jwk)

        assertEquals(0x30.toByte(), der[0], "DER should start with SEQUENCE tag")

        val rsaOid = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)
        assertTrue(der.containsSubArray(rsaOid), "DER should contain RSA OID")
        assertTrue(der.size > 10, "DER output should have meaningful length")
    }

    @Test
    fun testEcJwkToDer() {
        val ecJwkJson = """
            {
                "kty": "EC",
                "crv": "P-256",
                "x": "WbbFdwPNPKxKC65SXFR4ZmcWXJc8AiLvGkDBM3yRHCQ",
                "y": "bSiKv2RZGqgPd4pV_9Qb5oJxCw6xLqg4Z4Fvi5IjlLE",
                "alg": "ES256",
                "kid": "test-ec-key",
                "key_ops": ["verify"]
            }
        """.trimIndent()

        val jwk = JWK(Json.decodeFromString<JsonObject>(ecJwkJson))
        val der = ecJwkToDer(jwk)

        assertEquals(0x30.toByte(), der[0], "DER should start with SEQUENCE tag")

        val ecOid = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01)
        assertTrue(der.containsSubArray(ecOid), "DER should contain EC OID")

        val p256Oid = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)
        assertTrue(der.containsSubArray(p256Oid), "DER should contain P-256 curve OID")
        assertTrue(der.any { it == 0x04.toByte() }, "DER should contain uncompressed point marker")
    }

    @Test
    fun testEcdsaRawToDer() {
        val r = ByteArray(32) { (it + 1).toByte() }
        val s = ByteArray(32) { (it + 33).toByte() }
        val rawSignature = r + s

        val der = ecdsaRawToDer(rawSignature)

        assertEquals(0x30.toByte(), der[0], "DER should start with SEQUENCE tag")
        assertEquals(0x02.toByte(), der[2], "First element should be INTEGER tag for r")
        assertTrue(der.size >= rawSignature.size + 6, "DER should include tags and lengths overhead")
    }

    @Test
    fun testEcdsaRawToDerWithLeadingHighBit() {
        val r = ByteArray(32) { 0xFF.toByte() }
        val s = ByteArray(32) { 0x7F.toByte() }
        val rawSignature = r + s

        val der = ecdsaRawToDer(rawSignature)

        assertTrue(der.size >= 70, "DER should handle high bit padding")
        assertEquals(0x30.toByte(), der[0], "DER should start with SEQUENCE tag")
    }

    @Test
    fun testEcdsaRawToDerPreservesComponents() {
        val r = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val s = byteArrayOf(0x05, 0x06, 0x07, 0x08)
        val rawSignature = r + s

        val der = ecdsaRawToDer(rawSignature)

        assertTrue(der.containsSubArray(r), "DER should contain r component")
        assertTrue(der.containsSubArray(s), "DER should contain s component")
    }

    @Test
    fun testAsn1LengthEncodingShort() {
        val r = ByteArray(4) { 0x01 }
        val s = ByteArray(4) { 0x02 }
        val rawSignature = r + s

        val der = ecdsaRawToDer(rawSignature)

        assertEquals(14, der.size, "Short form length should be used for small signatures")
    }

    private fun ByteArray.containsSubArray(sub: ByteArray): Boolean {
        if (sub.isEmpty()) return true
        outer@ for (i in 0..(this.size - sub.size)) {
            for (j in sub.indices) {
                if (this[i + j] != sub[j]) continue@outer
            }
            return true
        }
        return false
    }
}
