package io.supabase.realtime.postgres

import io.supabase.postgrest.query.filter.FilterOperation
import io.supabase.postgrest.query.filter.escapedValue
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.postgresChangeFlow

typealias NegatableFilterOperation = Pair<Boolean, FilterOperation>

/**
 * Fluent builder for Postgres Changes `filter` strings.
 *
 * Each method appends a single `column=operator.value` condition. Multiple
 * conditions are combined with commas, which the Realtime server applies as an
 * `AND`. Use the DSL in [RealtimeChannel.postgresChangeFlow] — the
 * SDK serializes it to a string automatically — or call [build] to obtain
 * the string yourself.
 *
 * The builder mirrors the `postgrest-kt` filter API (`eq`, `neq`, `in`, `like`,
 * `not`, …) for the operators that Realtime supports. Values containing reserved
 * characters (`,`, `(`, `)`, `"`, `\`) — or surrounding whitespace — are
 * automatically double-quoted and escaped the same way PostgREST does, so they
 * survive the server's filter parser; all other values are sent verbatim.
 *
 */
class RealtimePostgresFilterBuilder {

    @PublishedApi
    internal val filters = mutableListOf<NegatableFilterOperation>()

    private fun add(column: String, operator: RealtimePostgresChangesFilterOperator, value: Any, negate: Boolean = false) {
        val operation = FilterOperation(column, operator.toPostgrestOperator(), value)
        filters.add(negate to operation)
    }

    /** Match rows where [column] equals [value] (`column=eq.value`). */
    fun eq(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.EQ, value)
    }

    /** Match rows where [column] does not equal [value] (`column=neq.value`). */
    fun neq(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.NEQ, value)
    }

    /** Match rows where [column] is greater than [value] (`column=gt.value`). */
    fun gt(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.GT, value)
    }

    /** Match rows where [column] is greater than or equal to [value] (`column=gte.value`). */
    fun gte(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.GTE, value)
    }

    /** Match rows where [column] is less than [value] (`column=lt.value`). */
    fun lt(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.LT, value)
    }

    /** Match rows where [column] is less than or equal to [value] (`column=lte.value`). */
    fun lte(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.LTE, value)
    }


    /**
     * Match rows where [column] is one of [values] (`column=in.(a,b,c)`).
     * Requires at least one value; duplicates are removed. An element containing a
     * reserved character is double-quoted (`in.("a,b",c)`), so commas inside an
     * element are preserved. `null` is intentionally not accepted (`IN (null)`
     * never matches in SQL) — use `is`/`not('col','is',null)` for null checks.
     */
    fun isIn(column: String, values: List<Any>) {
        add(column, RealtimePostgresChangesFilterOperator.IN, values)
    }

    /** Match rows where [column] matches the case-sensitive [pattern] (`column=like.pattern`). */
    fun like(column: String, pattern: String) {
        add(column, RealtimePostgresChangesFilterOperator.LIKE, pattern)
    }

    /** Match rows where [column] matches the case-insensitive [pattern] (`column=ilike.pattern`). */
    fun ilike(column: String, pattern: String) {
        add(column, RealtimePostgresChangesFilterOperator.ILIKE, pattern)
    }

    /** Match rows where [column] matches the POSIX regex [pattern] (`column=match.pattern`). */
    fun match(column: String, pattern: String) {
        add(column, RealtimePostgresChangesFilterOperator.MATCH, pattern)
    }

    /** Match rows where [column] matches the case-insensitive POSIX regex `pattern` (`column=imatch.pattern`). */
    fun imatch(column: String, pattern: String) {
        add(column, RealtimePostgresChangesFilterOperator.IMATCH, pattern)
    }

    /**
     * Match rows where [column] `IS` the given value (`column=is.null`).
     * Accepts `null`, a boolean, or the keywords `'null' | 'true' | 'false' | 'unknown'`.
     */
    fun exact(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.IS, value)
    }

    /** Match rows where [column] is distinct from [value] (`column=isdistinct.value`). NULL-safe inequality. */
    fun isDistinct(column: String, value: Any) {
        add(column, RealtimePostgresChangesFilterOperator.ISDISTINCT, value)
    }

    /**
     * Negate any operator with the `not.` prefix (`column=not.operator.value`).
     * `in` takes an array, `is` takes an `IS` keyword/boolean/null, and every
     * other operator takes a scalar value.
     */
    fun not(column: String, operator: RealtimePostgresChangesFilterOperator, value: Any) {
        add(column, operator, value, true)
    }

    /**
     * Serialize all conditions into the comma-separated (AND) filter string.
     *
     * Conditions are joined by commas, which the server applies as `AND`. A scalar
     * value (or single `in` element) that contains a reserved character — `,`,
     * `(`, `)`, `"`, `\` — or surrounding whitespace is double-quoted and escaped
     * the way PostgREST does, so commas inside a value are preserved rather than
     * read as a condition boundary.
     */
    fun build() = filters.joinToString(",") { (negate, operation) ->
        val prefix = if (negate) "not." else ""
        "${operation.column}=$prefix${operation.operator.name.lowercase()}.${operation.escapedValue(true)}"
    }

}