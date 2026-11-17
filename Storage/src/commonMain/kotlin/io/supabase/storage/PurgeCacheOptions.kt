package io.supabase.storage

/**
 * Options for purging the cache in Supabase Storage.
 */
class PurgeCacheOptions {

    /**
     * If true, purges only the transformations (resized/formatted variants) for the object or bucket,
     * leaving the original cached file intact. If omitted, purges all cached versions.
     */
    var transformations: Boolean = true

}