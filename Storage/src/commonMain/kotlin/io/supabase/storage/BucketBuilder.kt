@file:Suppress("UndocumentedPublicProperty")
package io.supabase.storage

import io.ktor.http.ContentType
import kotlin.jvm.JvmName

/**
 * A builder for [Bucket]s
 */
class BucketBuilder {

    /**
     * Whether the bucket should be public
     */
    var public: Boolean? = null

    /**
     * The file size limit for the bucket. E.g. **10.megabytes**
     */
    var fileSizeLimit: FileSizeLimit? = null

    /**
     * The allowed mime types for the bucket
     */
    internal var allowedMimeTypes: List<String>? = null


    /**
     * Sets the allowed mime types for the bucket
     */
    fun allowedMimeTypes(vararg mimeTypes: String) {
        allowedMimeTypes = mimeTypes.toList()
    }

    /**
     * Sets the allowed mime types for the bucket
     */
    fun allowedMimeTypes(mimeTypes: List<String>) {
        allowedMimeTypes = mimeTypes
    }

    /**
     * Sets the allowed mime types for the bucket
     */
    @JvmName("allowedMimeTypesContentType")
    fun allowedMimeTypes(mimeTypes: List<ContentType>) {
        allowedMimeTypes = mimeTypes.map { it.toString() }
    }

    /**
     * Sets the allowed mime types for the bucket
     */
    fun allowedMimeTypes(vararg mimeTypes: ContentType) {
        allowedMimeTypes = mimeTypes.map { it.toString() }
    }

    val Long.bytes get() = FileSizeLimit("${this}b")
    val Long.kilobytes get() = FileSizeLimit("${this}kb")
    val Long.megabytes get() = FileSizeLimit("${this}mb")
    val Long.gigabytes get() = FileSizeLimit("${this}gb")

    val Int.bytes get() = FileSizeLimit("${this}b")
    val Int.kilobytes get() = FileSizeLimit("${this}kb")
    val Int.megabytes get() = FileSizeLimit("${this}mb")
    val Int.gigabytes get() = FileSizeLimit("${this}gb")

}