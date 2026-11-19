package io.supabase.storage

import io.supabase.annotations.SupabaseInternal

/**
 * Interface for bucket file url modifiers
 */
interface BucketUrlBuilder {

    /**
     * Append a cache nonce parameter to the URL to invalidate the cache.
     */
    var cacheNonce: String?

    /**
     * Builders where you can apply a force download
     */
    interface WithForceDownload : BucketUrlBuilder {

        @SupabaseInternal
        var download: ForceDownload?

        /**
         * Forces a download when opening the created url.
         * @param fileName Optional file name. Set this parameter as the name of the file if you want to trigger the download with a different filename.
         */
        fun forceDownload(fileName: String? = null) {
            download = ForceDownload(fileName)
        }
    }

    /**
     * Builders where you can apply a transformation
     */
    interface WithTransformation : BucketUrlBuilder {

        @SupabaseInternal
        var transformation: ImageTransformation?

        /**
         * Transforms the image before downloading
         * @param transform The transformation to apply
         */
        fun transform(transform: ImageTransformation.() -> Unit) {
            this.transformation = ImageTransformation().apply(transform)
        }
    }
}

/**
 * Builder for [BucketApi.createSignedUrl]
 */
class SignedUrlBuilder : BucketUrlBuilder.WithTransformation, BucketUrlBuilder.WithForceDownload {
    @SupabaseInternal
    override var transformation: ImageTransformation? = null
    override var download: ForceDownload? = null
    override var cacheNonce: String? = null
}

/**
 * Builder for [BucketApi.createSignedUrls]
 */
class SignedUrlsBuilder : BucketUrlBuilder.WithForceDownload {
    @SupabaseInternal
    override var download: ForceDownload? = null
    override var cacheNonce: String? = null
}

/**
 * Builder for [BucketApi.publicUrl]
 */
class PublicUrlBuilder: BucketUrlBuilder.WithTransformation, BucketUrlBuilder.WithForceDownload {
    @SupabaseInternal
    override var transformation: ImageTransformation? = null
    override var cacheNonce: String? = null

    @SupabaseInternal
    override var download: ForceDownload? = null
}
