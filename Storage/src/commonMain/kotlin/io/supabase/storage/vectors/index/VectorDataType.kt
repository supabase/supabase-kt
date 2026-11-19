package io.supabase.storage.vectors.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supported data types for vectors.
 *
 * Currently only [FLOAT32] is supported.
 * @property value Lowercase API representation of the data type.
 */
@Serializable
enum class VectorDataType {
    /** 32-bit floating point vector data type. */
    @SerialName("float32")
    FLOAT32;

    /** Lowercase API representation of the data type. */
    val value = this.name.lowercase()
}