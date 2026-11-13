package io.supabase.storage

import io.supabase.network.HttpRequestOverride

/**
 * Builder for downloading files with additional options
 */
class DownloadOptionBuilder(
    internal var transform: ImageTransformation.() -> Unit = {},
    internal val httpRequestOverrides: MutableList<HttpRequestOverride> = mutableListOf()
): BucketUrlBuilder {

    override var cacheNonce: String? = null

    /**
     * Transforms the image before downloading
     * @param transform The transformation to apply
     */
    fun transform(transform: ImageTransformation.() -> Unit) {
        this.transform = transform
    }

    /**
     * Overrides the HTTP request
     * @param override The override to apply
     */
    fun httpOverride(override: HttpRequestOverride) {
        httpRequestOverrides.add(override)
    }
}