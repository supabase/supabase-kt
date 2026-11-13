package io.supabase.storage


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Represents a storage bucket
 * @param createdAt The creation date of the bucket
 * @param id The id of the bucket
 * @param name The name of the bucket
 * @param owner The owner of the bucket
 * @param updatedAt The last update date of the bucket
 * @param public Whether the bucket is public
 * @param allowedMimeTypes The allowed mime types for the bucket
 * @param fileSizeLimit The file size limit for the bucket
 * @param type The bucket type
 * @see BucketBuilder
 * @see Storage.createBucket
 */
@Serializable
data class Bucket(
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("owner")
    val owner: String,
    @SerialName("updated_at")
    val updatedAt: Instant,
    val public: Boolean,
    @SerialName("allowed_mime_types")
    val allowedMimeTypes: List<String>? = null,
    @SerialName("file_size_limit")
    val fileSizeLimit: Long? = null,
    val type: BucketType? = null
)

/**
 * Type of storage bucket
 */
enum class BucketType {
    /**
     * Regular file storage buckets
     */
    STANDARD,
    /**
     * Iceberg table-based buckets for analytical workloads
     */
    ANALYTICS
}