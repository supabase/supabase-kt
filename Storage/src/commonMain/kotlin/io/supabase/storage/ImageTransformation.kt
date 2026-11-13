package io.supabase.storage

import io.supabase.annotations.SupabaseInternal
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder

/**
 * Represents a transformation for an image. Used for [Storage] objects
 * @see Resize
 * @see BucketApi.downloadAuthenticated
 * @see BucketApi.downloadPublic
 */
class ImageTransformation {

    /**
     * The width of the image
     */
    var width: Int? = null

    /**
     * The height of the image
     */
    var height: Int? = null

    /**
     * The quality of the image. (Percentage 1-100, defaults to 80)
     */
    var quality: Int? = null
        set(value) {
            require(value in VALID_QUALITY_RANGE) { "Quality must be between 1 and 100"}
            field = value
        }

    /**
     * Specify in which format you want the image to receive. (Defaults to 'origin', which means the original format)
     */
    var format: String? = null

    /**
     * The resize mode
     */
    var resize: Resize? = null

    /**
     * Changes the size of the image
     * @param width The width of the image
     * @param height The height of the image
     */
    fun size(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    /**
     * Resizes the image while keeping the aspect ratio to fill a given size and crops projecting parts
     */
    fun cover() {
        resize = Resize.COVER
    }

    /**
     * Resizes the image while keeping the aspect ratio to fit a given size.
     */
    fun contain() {
        resize = Resize.CONTAIN
    }

    /**
     * Resizes the image without keeping the aspect ratio.
     */
    fun fill() {
        resize = Resize.FILL
    }

    internal fun query(): Parameters {
        val builder = ParametersBuilder()
        width?.let { builder.append("width", it.toString()) }
        height?.let { builder.append("height", it.toString()) }
        resize?.let { builder.append("resize", it.name.lowercase()) }
        quality?.let { builder.append("quality", it.toString()) }
        format?.let { builder.append("format", it) }
        return builder.build()
    }

    /**
     * The resize mode
     */
    enum class Resize {
        /**
         * Resizes the image while keeping the aspect ratio to fill a given size and crops projecting parts
         */
        COVER,

        /**
         * Resizes the image while keeping the aspect ratio to fit a given size.
         */
        CONTAIN,

        /**
         * Resizes the image without keeping the aspect ratio.
         */
        FILL
    }

    companion object {
        @SupabaseInternal
        val VALID_QUALITY_RANGE = 1..100
    }

}