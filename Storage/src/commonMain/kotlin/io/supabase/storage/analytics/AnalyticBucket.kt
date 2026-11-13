package io.supabase.storage.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Represents an Analytics Bucket using Apache Iceberg table format.
 * Analytics buckets are optimized for analytical queries and data processing.
 * @param name Unique identifier for the bucket
 * @param type Bucket type - always 'ANALYTICS' for analytics buckets.
 * @param format Storage format used. Defaults to 'iceberg'.
 * @param createdAt Timestamp of bucket creation
 * @param updatedAt Timestamp of last update
 */
@Serializable
data class AnalyticBucket(
    val name: String,
    val type: String = "ANALYTICS",
    val format: String = "iceberg",
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant
)
