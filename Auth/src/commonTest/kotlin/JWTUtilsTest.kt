import io.supabase.auth.jwt.JWTUtils
import io.supabase.auth.jwt.JwtHeader
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class JWTUtilsTest {

    @Test
    fun testDecodeJwt() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"
        //val secret = "a-string-secret-at-least-256-bits-long"
        val (claims, header, signature) = JWTUtils.decodeJwt(jwt).claimsResponse
        assertEquals(JwtHeader.Algorithm.HS256, header.alg)
        assertEquals("JWT", header.typ)
        assertEquals("1234567890", claims.sub)
        assertEquals("John Doe", claims.getClaim<String>("name"))
        assertTrue { claims.getClaim<Boolean>("admin") }
        assertEquals(Instant.fromEpochSeconds(1516239022), claims.iat)
        assertContentEquals(JWTUtils.decodeBase64ByteArray("KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"), signature)
    }

}