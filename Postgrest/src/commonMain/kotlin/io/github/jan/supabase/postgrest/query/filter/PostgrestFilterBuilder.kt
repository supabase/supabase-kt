@file:Suppress("UndocumentedPublicProperty", "ConstructorParameterNaming")
package io.github.jan.supabase.postgrest.query.filter

import io.github.jan.supabase.auth.PostgrestFilterDSL
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import kotlin.reflect.KProperty1

/**
 * A builder for Postgrest filters
 */
@PostgrestFilterDSL
class PostgrestFilterBuilder(
    @PublishedApi internal val propertyConversionMethod: PropertyConversionMethod,
    @PublishedApi internal val _params: MutableMap<String, List<String>> = mutableMapOf(),
    val isInLogicalExpression: Boolean = false
) {

    val params: Map<String, List<String>>
        get() = _params.toMap()

    /**
     * Adds a negated filter to the query
     */
    fun filterNot(column: String, operator: FilterOperator, value: Any?) = filterNot(FilterOperation(column, operator, value))

    /**
     * Adds a negated filter to the query
     */
    fun filterNot(operation: FilterOperation) {
        val columnValue = params[operation.column] ?: emptyList()
        _params[operation.column] = columnValue + listOf("not.${operation.operator.identifier}.${operation.escapedValue(isInLogicalExpression)}")
    }

    /**
     * Adds a filter to the query
     */
    fun filter(column: String, operator: FilterOperator, value: Any?) {
        filter(FilterOperation(column, operator, value))
    }

    /**
     * Adds a filter to the query
     */
    fun filter(operation: FilterOperation) {
        val columnValue = params[operation.column] ?: emptyList()
        _params[operation.column] = columnValue + listOf("${operation.operator.identifier}.${operation.escapedValue(isInLogicalExpression)}")
    }

    /**
     * Finds all rows where the value of the [column] is equal to [value]
     */
    fun eq(column: String, value: Any) = filter(column, FilterOperator.EQ, value)

    /**
     * Finds all rows where the value of the [column] is not equal to [value]
     */
    fun neq(column: String, value: Any) = filter(column, FilterOperator.NEQ, value)

    /**
     * Finds all rows where the value of the [column] is greater than [value]
     */
    fun gt(column: String, value: Any) = filter(column, FilterOperator.GT, value)

    /**
     * Finds all rows where the value of the [column] is greater than or equal to [value]
     */
    fun gte(column: String, value: Any) = filter(column, FilterOperator.GTE, value)

    /**
     * Finds all rows where the value of the [column] is less than or equal to [value]
     */
    fun lte(column: String, value: Any) = filter(column, FilterOperator.LTE, value)

    /**
     * Finds all rows where the value of the [column] is less than [value]
     */
    fun lt(column: String, value: Any) = filter(column, FilterOperator.LT, value)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern]
     */
    fun like(column: String, pattern: String) = filter(column, FilterOperator.LIKE, pattern)

    /**
     * Finds all rows where the value of the [column] matches all of the specified [patterns]
     *
     * An empty [patterns] list matches every row, because PostgreSQL evaluates `ALL` over an empty array as true.
     * Check the list before using this filter in a `delete` or `update`.
     */
    fun likeAll(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("like(all).{${patterns.joinToString(",") { escapeArrayLiteralElement(it) }}}")
    }

    /**
     * Finds all rows where the value of the [column] matches any of the specified [patterns]
     *
     * An empty [patterns] list matches no rows, because PostgreSQL evaluates `ANY` over an empty array as false.
     */
    fun likeAny(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("like(any).{${patterns.joinToString(",") { escapeArrayLiteralElement(it) }}}")
    }

    /**
     * Finds all rows where the value of the [column] matches all of the specified [patterns] (case-insensitive)
     *
     * An empty [patterns] list matches every row, because PostgreSQL evaluates `ALL` over an empty array as true.
     * Check the list before using this filter in a `delete` or `update`.
     */
    fun ilikeAll(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("ilike(all).{${patterns.joinToString(",") { escapeArrayLiteralElement(it) }}}")
    }

    /**
     * Finds all rows where the value of the [column] matches any of the specified [patterns] (case-insensitive)
     *
     * An empty [patterns] list matches no rows, because PostgreSQL evaluates `ANY` over an empty array as false.
     */
    fun ilikeAny(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("ilike(any).{${patterns.joinToString(",") { escapeArrayLiteralElement(it) }}}")
    }

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] (case-insensitive)
     */
    fun ilike(column: String, pattern: String) = filter(column, FilterOperator.ILIKE, pattern)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] using pattern matching
     */
    fun regexMatch(column: String, pattern: String) = filter(column, FilterOperator.MATCH, pattern)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] using pattern matching (case-insensitive)
     */
    fun regexIMatch(column: String, pattern: String) = filter(column, FilterOperator.IMATCH, pattern)

    /**
     * Match only rows where each column in `query` keys is equal to its
     * associated value. Shorthand for multiple `.eq()`s.
     *
     * @param query The object to filter with, with column names as keys mapped
     * to their filter values
     */
    fun match(query: Map<String, Any>) {
        for((column, value) in query) {
            eq(column, value)
        }
    }

    /**
     * Finds all rows where the value of the [column] equals to one of these values: null,true,false,unknown
     */
    fun exact(column: String, value: Boolean?) = filter(column, FilterOperator.IS, value)

    /**
     * Finds all rows where the value of the [column] is a member of [values]
     */
    fun isIn(column: String, values: List<Any>) = filter(column, FilterOperator.IN, values)

    /**
     * Finds all rows where the value of the [column] is not a member of [values]
     */
    fun notIn(column: String, values: List<Any>) = filterNot(column, FilterOperator.IN, values)

    /**
     * Finds all rows where the value of the [column] is strictly left of [range]
     */
    fun sl(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.SL, range)

    /**
     * Finds all rows where the value of the [column] is strictly right of [range]
     */
    fun sr(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.SR, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the left of [range]
     */
    fun nxl(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.NXL, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun nxr(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.NXR, range)

    /**
     * Finds all rows where the value of the [column] is strictly left of [range]
     */
    fun rangeLte(column: String, range: Pair<Any, Any>) = nxr(column, range)

    /**
     * Finds all rows where the value of the [column] is strictly right of [range]
     */
    fun rangeGte(column: String, range: Pair<Any, Any>) = nxl(column, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun rangeLt(column: String, range: Pair<Any, Any>) = sl(column, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun rangeGt(column: String, range: Pair<Any, Any>) = sr(column, range)

    /**
     * Finds all rows where the value of the [column] is adjacent to [range]
     */
    fun adjacent(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.ADJ, range)

    /**
     * Adds an `or` condition to the query
     * @param negate Whether to negate the condition
     * @param referencedTable The table to reference
     */
    @PostgrestFilterDSL
    inline fun or(negate: Boolean = false, referencedTable: String? = null, filter: @PostgrestFilterDSL PostgrestFilterBuilder.() -> Unit) {
        val prefix = buildString {
            if(negate) append("not.")
            if(referencedTable != null) append("$referencedTable.")
        }
        val joiningFilter = formatJoiningFilter(filter)
        if(joiningFilter == "()") return //empty logical expressions return a postgrest error
        _params[prefix + "or"] = listOf(joiningFilter) + if(isInLogicalExpression) _params[prefix + "or"] ?: emptyList() else emptyList()
    }

    /**
     * Adds an `and` condition to the query
     * @param negate Whether to negate the condition
     * @param referencedTable The table to reference
     */
    @PostgrestFilterDSL
    inline fun and(negate: Boolean = false, referencedTable: String? = null, filter: @PostgrestFilterDSL PostgrestFilterBuilder.() -> Unit) {
        val prefix = buildString {
            if(negate) append("not.")
            if(referencedTable != null) append("$referencedTable.")
        }
        val joiningFilter = formatJoiningFilter(filter)
        if(joiningFilter == "()") return //empty logical expressions return a postgrest error
        _params[prefix + "and"] = listOf(joiningFilter) + if(isInLogicalExpression) _params[prefix + "and"] ?: emptyList() else emptyList()
    }

    /**
     * Runs a full text search on [column] with the specified [query] and [textSearchType]
     */
    fun textSearch(column: String, query: String, textSearchType: TextSearchType, config: String? = null): PostgrestFilterBuilder {
        val configPart = if (config === null) "" else "(${config})"
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("${textSearchType.identifier}fts${configPart}.${query}")
        return this
    }

    /**
     * Finds all rows where the value of the [column] contains [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun contains(column: String, values: List<Any>) = filter(column, FilterOperator.CS, values)

    /**
     * Finds all rows where the value of the [column] is contained in [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun contained(column: String, values: List<Any>) = filter(column, FilterOperator.CD, values)

    /**
     * Finds all rows where the value of the [column] contains [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun cs(column: String, values: List<Any>) = contains(column, values)

    /**
     * Finds all rows where the value of the [column] is contained in [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun cd(column: String, values: List<Any>) = contained(column, values)

    /**
     * Finds all rows where the value of the [column] overlaps with [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun ov(column: String, values: List<Any>) = overlaps(column, values)

    /**
     * Finds all rows where the value of the [column] overlaps with [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun overlaps(column: String, values: List<Any>) = filter(column, FilterOperator.OV, values)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.eq(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.EQ, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is not equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.neq(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.NEQ, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is greater than [value]
     */
    infix fun <T, V> KProperty1<T, V>.gt(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.GT, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is greater than or equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.gte(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.GTE, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is less than [value]
     */
    infix fun <T, V> KProperty1<T, V>.lt(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LT, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is less than or equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.lte(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LTE, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     */
    infix fun <T, V> KProperty1<T, V>.like(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LIKE, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     * using pattern matching
     */
    infix fun <T, V> KProperty1<T, V>.match(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.MATCH, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern] (case-insensitive)
     */
    infix fun <T, V> KProperty1<T, V>.ilike(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.ILIKE, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     * using pattern matching
     */
    infix fun <T, V> KProperty1<T, V>.imatch(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IMATCH, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] equals to one of these values: null,true,false,unknown
     */
    infix fun <T, V> KProperty1<T, V>.isExact(value: Boolean?) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IS, value))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is in the specified [list]
     */
    infix fun <T, V> KProperty1<T, V>.isIn(list: List<V>) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IN, list))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is not in the specified [list]
     */
    infix fun <T, V> KProperty1<T, V>.notIn(list: List<V>) = filterNot(FilterOperation(propertyConversionMethod(this), FilterOperator.IN, list))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is strictly left of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeLt(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeLt(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not extend to the left of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeLte(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeLte(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not extend to the right of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeGt(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeGt(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not strictly right of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeGte(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeGte(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is adjacent to the specified [range]
     */
    infix fun <T, V> KProperty1<T, V>.adjacent(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.adjacent(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] overlaps with [values]
     */
    infix fun <T, V> KProperty1<T, V>.overlaps(values: List<Any>) = this@PostgrestFilterBuilder.overlaps(propertyConversionMethod(this), values)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] contains [values]
     */
    infix fun <T, V> KProperty1<T, V>.contains(values: List<Any>) = this@PostgrestFilterBuilder.contains(propertyConversionMethod(this), values)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is contained in [values]
     */
    infix fun <T, V> KProperty1<T, V>.contained(values: List<Any>) = this@PostgrestFilterBuilder.contained(propertyConversionMethod(this), values)


}

@PublishedApi internal inline fun PostgrestFilterBuilder.formatJoiningFilter(filter: PostgrestFilterBuilder.() -> Unit): String {
    val params = PostgrestFilterBuilder(propertyConversionMethod, isInLogicalExpression = true).apply(filter).params
    val formattedFilter = params.toList().joinToString(",") {
        it.second.joinToString(",") { filter ->
            val isLogicalOperator = filter.startsWith("(") && filter.endsWith(")")
            it.first + (if(isLogicalOperator) "" else ".") + filter
        }
    }
    return "($formattedFilter)"
}
