package io.supabase.storage

import io.supabase.storage.analytics.StorageAnalyticsClient
import io.ktor.http.parameters
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * A filter builder for [BucketApi.list] and [Storage.listBuckets]
 * @see Files
 * @see Buckets
 */
open class StorageListFilter {

    /**
     * The limit of items to return.
     */
    var limit: Int? = null

    /**
     * The number of items to skip before returning results. Useful for pagination.
     */
    var offset: Int? = null

    /**
     * The search string to filter items by.
     */
    var search: String? = null
    protected var sortColumn: String? = null
    protected var sortOrder: String? = null

    internal fun buildBody() = buildJsonObject {
        limit?.let {
            put("limit", it)
        }
        offset?.let {
            put("offset", it)
        }
        search?.let {
            put("search", it)
        }
        sortColumn?.let {
            putJsonObject("sortBy") {
                put("column", sortColumn)
                put("order", sortOrder)
            }
        }
    }

    internal fun buildParameters() = parameters {
        limit?.let { set("limit", it.toString()) }
        offset?.let { set("offset", it.toString()) }
        search?.let { set("search", it) }
        sortOrder?.let { set("sortOrder", it) }
        sortColumn?.let { set("sortColumn", it) }
    }

    /**
     * Filter for [BucketApi.list]
     */
    class Files : StorageListFilter() {

        /**
         * Sorts the result by the given [column] in the given [order]
         * @param column The column to sort by
         * @param order The sort order (ascending or descending)
         */
        fun sortBy(column: String, order: SortOrder) {
            this.sortColumn = column
            this.sortOrder = order.name.lowercase()
        }

    }

    /**
     * Filter for [Storage.listBuckets] and [StorageAnalyticsClient.listBuckets]
     */
    class Buckets : StorageListFilter() {

        /**
         * Sets the sorting criteria for the bucket list results
         * @param column The column to sort by
         * @param order The sort order (ascending or descending)
         */
        fun sortBy(column: BucketSortColumn, order: SortOrder) {
            this.sortColumn = column.name.lowercase()
            this.sortOrder = order.name.lowercase()
        }

    }

}

/**
 * Represents the available columns for sorting bucket results.
 */
enum class BucketSortColumn {
    /** Sort by bucket ID */
    ID,

    /** Sort by bucket name */
    NAME,

    /** Sort by bucket creation timestamp */
    CREATED_AT,

    /** Sort by bucket last updated timestamp */
    UPDATED_AT
}