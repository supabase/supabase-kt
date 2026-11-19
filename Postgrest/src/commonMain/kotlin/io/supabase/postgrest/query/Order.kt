package io.supabase.postgrest.query

/**
 * Used to order the result of a query
 * @param value The value to be used in the query
 */
enum class Order(val value: String) {
    /**
     * Order the data ascending
     */
    ASCENDING("asc"),

    /**
     * Order the data descending
     */
    DESCENDING("desc");
}
