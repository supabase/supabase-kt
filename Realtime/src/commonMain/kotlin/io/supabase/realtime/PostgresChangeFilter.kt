package io.supabase.realtime

import io.supabase.annotations.SupabaseInternal
import io.supabase.postgrest.query.filter.FilterOperation
import io.supabase.postgrest.query.filter.FilterOperator
import io.supabase.postgrest.query.filter.escapedValue
import io.supabase.realtime.postgres.RealtimePostgresFilterBuilder

/**
 * Used to filter postgres changes
 */
class PostgresChangeFilter(private val event: String, private val schema: String) {

    /**
     * The table name that should be monitored
     */
    var table: String? = null

    /**
     * Filter the received changes in your table.
     * E.g.: "user_id=eq.1"
     */
    var filter: String? = null

    /**
     * Filters the received changes in your table.
     * @param filter The filter operation. Note that only some operators are supported. See [Postgres Changes](https://supabase.com/docs/guides/realtime/postgres-changes?language=kotlin#available-filters) for more information.
     */
    fun filter(filter: FilterOperation) {
        val filterValue = when(filter.operator) {
            FilterOperator.EQ,
            FilterOperator.NEQ,
            FilterOperator.GT,
            FilterOperator.GTE,
            FilterOperator.LT,
            FilterOperator.LTE,
            FilterOperator.IN ->
                filter.escapedValue(false)
            else -> throw UnsupportedOperationException("Unsupported filter operator: ${filter.operator}")
        }
        this.filter = "${filter.column}=${filter.operator.name.lowercase()}.$filterValue"
    }

    /**
     * Filters the received changes in your table.
     * @param column The column name
     * @param operator The filter operator. Note that only some operators are supported. See [Postgres Changes](https://supabase.com/docs/guides/realtime/postgres-changes?language=kotlin#available-filters) for more information.
     * @param value The value to filter for. This can be context dependent. E.g. for the `IN` operator this can be a list of values, however you can also provide a String.
     */
    fun filter(column: String, operator: FilterOperator, value: Any) {
        filter(FilterOperation(column, operator, value))
    }

    /**
     * Fluent builder for Postgres Changes `filter` strings.
     *
     * Each method appends a single `column=operator.value` condition. Multiple
     * conditions are combined with commas, which the Realtime server applies as an
     * `AND`.
     *
     * The builder mirrors the `postgrest-kt` filter API (`eq`, `neq`, `in`, `like`,
     * `not`, …) for the operators that Realtime supports. Values containing reserved
     * characters (`,`, `(`, `)`, `"`, `\`) — or surrounding whitespace — are
     * automatically double-quoted and escaped the same way PostgREST does, so they
     * survive the server's filter parser; all other values are sent verbatim.
     *
     */
    inline fun filter(builder: RealtimePostgresFilterBuilder.() -> Unit) {
        val builder = RealtimePostgresFilterBuilder().apply(builder)
        filter = builder.build()
    }

    /**
     * Fluent builder for Postgres Changes `filter` strings.
     *
     * Each method appends a single `column=operator.value` condition. Multiple
     * conditions are combined with commas, which the Realtime server applies as an
     * `AND`.
     *
     * The builder mirrors the `postgrest-kt` filter API (`eq`, `neq`, `in`, `like`,
     * `not`, …) for the operators that Realtime supports. Values containing reserved
     * characters (`,`, `(`, `)`, `"`, `\`) — or surrounding whitespace — are
     * automatically double-quoted and escaped the same way PostgREST does, so they
     * survive the server's filter parser; all other values are sent verbatim.
     *
     */
    fun filter(builder: RealtimePostgresFilterBuilder) {
        filter = builder.build()
    }

    @SupabaseInternal
    fun buildConfig() = PostgresJoinConfig(schema, table, filter, event)

}
