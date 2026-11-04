@file:Suppress("MatchingDeclarationName")
package io.supabase.auth

import dev.whyoleg.cryptography.random.CryptographyRandom
import okio.ByteString.Companion.toByteString
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal object PKCEConstants {
    const val VERIFIER_LENGTH = 64
    const val CHALLENGE_METHOD = "s256"
}

@OptIn(ExperimentalEncodingApi::class)
internal fun generateCodeVerifier(): String {
    val bytes = ByteArray(PKCEConstants.VERIFIER_LENGTH)
    CryptographyRandom.nextBytes(bytes)
    return Base64.UrlSafe.encode(bytes)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun generateCodeChallenge(codeVerifier: String): String {
    val byteString = codeVerifier.encodeToByteArray().toByteString()
    val hash = byteString.sha256()
    return Base64.UrlSafe.encode(hash.toByteArray()).replace("=", "")
}