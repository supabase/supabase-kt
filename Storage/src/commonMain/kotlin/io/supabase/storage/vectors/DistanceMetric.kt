package io.supabase.storage.vectors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Distance metrics for vector similarity search.
 *
 * @property value Lowercase API representation of the distance metric.
 */
@Serializable
enum class DistanceMetric {
    /** Cosine distance metric. */
    @SerialName("cosine")
    COSINE,

    /** Euclidean distance metric. */
    @SerialName("euclidean")
    EUCLIDEAN,

    /** Dot product distance metric. */
    @SerialName("dotproduct")
    DOTPRODUCT;

    /** Lowercase API representation of the distance metric. */
    val value = this.name.lowercase()
}