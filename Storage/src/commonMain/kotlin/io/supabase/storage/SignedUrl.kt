package io.supabase.storage

import kotlinx.serialization.Serializable

/**
 * Represents a signed url
 * @param error An optional error message, set when no url could be created for [path]
 * @param signedURL The signed url, empty if [error] is set
 * @param path The path of the file
 */
@Serializable
data class SignedUrl(val error: String? = null, val signedURL: String, val path: String)

// The server returns signedURL: null for paths that don't exist or can't be accessed
@Serializable
internal data class SignedUrlResponse(val error: String? = null, val signedURL: String? = null, val path: String)
