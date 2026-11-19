package io.supabase.storage.vectors.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Single vector object for insertion/update
 * @property key Unique identifier for the vector
 * @property data Vector embedding data
 * @property metadata Optional arbitrary metadata
 */
@Serializable
data class VectorObject(
    val key: String,
    val data: VectorData,
    val metadata: JsonObject
)

/**
 * Vector data representation
 * Vectors must be float32 arrays with dimensions matching the index
 * @property float32 Array of 32-bit floating point numbers
 */
@Serializable
class VectorData(
    val float32: FloatArray
)