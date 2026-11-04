@file:OptIn(ExperimentalTime::class)

package io.supabase.storage

import io.supabase.SupabaseSerializer
import io.supabase.decode
import io.supabase.serializer.KotlinXSerializer
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a file or a folder in a bucket. If the item is a folder, everything except [name] is null.
 * @param name The name of the item
 * @param id The id of the item
 * @param updatedAt The last update date of the item
 * @param createdAt The creation date of the item
 * @param lastAccessedAt The last access date of the item
 * @param metadata The metadata of the item
 */
@Serializable
data class FileObject(
    val name: String,
    val id: String?,
    @SerialName("updated_at")
    val updatedAt: Instant?,
    @SerialName("created_at")
    val createdAt: Instant?,
    @SerialName("last_accessed_at")
    val lastAccessedAt: Instant?,
    val metadata: JsonObject?
)

/**
 * Represents a file or a folder in a bucket. If the item is a folder, everything except [name] is null.
 * @param name The name of the item
 * @param id The id of the item
 * @param version The version of the item
 * @param bucketId The bucket id of the item
 * @param updatedAt The last update date of the item
 * @param createdAt The creation date of the item
 * @param lastAccessedAt The last access date of the item
 * @param metadata The metadata of the item
 * @param size The size of the item
 * @param rawContentType The content type of the item
 * @param etag The etag of the item
 * @param lastModified The last modified date of the item
 * @param cacheControl The cache control of the item
 * @param serializer The serializer to use for decoding the metadata
 */
@Serializable
data class FileObjectV2(
    val name: String,
    val id: String?,
    val version: String,
    @SerialName("bucket_id")
    val bucketId: String? = null,
    @SerialName("updated_at")
    val updatedAt: Instant? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("last_accessed_at")
    val lastAccessedAt: Instant? = null,
    val metadata: JsonObject? = null,
    val size: Long,
    @SerialName("content_type")
    val rawContentType: String,
    val etag: String? = null,
    @SerialName("last_modified")
    val lastModified: Instant? = null,
    @SerialName("cache_control")
    val cacheControl: String? = null,
    @Transient @PublishedApi internal val serializer: SupabaseSerializer = KotlinXSerializer()
) {

    /**
     * The content type of the file
     */
    val contentType by lazy {
        ContentType.parse(rawContentType)
    }

    /**
     * Decodes the metadata using the [serializer]
     */
    inline fun <reified T> decodeMetadata(): T? = metadata?.let { serializer.decode(it.toString()) }

}