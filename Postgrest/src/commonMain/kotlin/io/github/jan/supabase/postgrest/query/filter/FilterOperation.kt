@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)

package io.github.jan.supabase.postgrest.query.filter

/**
 * Represents a filter operation for a column using a specific operator and a value.
 */
data class FilterOperation(val column: String, val operator: FilterOperator, val value: Any?)

fun FilterOperation.escapedValue(isInLogicalExpression: Boolean): String =
    when (operator) {
        FilterOperator.EQ,
        FilterOperator.NEQ,
        FilterOperator.GT,
        FilterOperator.GTE,
        FilterOperator.LT,
        FilterOperator.LTE,
        FilterOperator.LIKE,
        FilterOperator.ILIKE,
        FilterOperator.MATCH,
        FilterOperator.IMATCH,
        FilterOperator.IS,
        FilterOperator.FTS,
        FilterOperator.WFTS,
        FilterOperator.PHFTS,
        FilterOperator.ISDISTINCT,
        FilterOperator.PLFTS -> encodeScalar(value, isInLogicalExpression)
        FilterOperator.IN ->
            if (value is List<*>) {
                encodeAsList(value)
            } else {
                escapeValue(value)
            }
        FilterOperator.CS,
        FilterOperator.CD,
        FilterOperator.OV ->
            when (value) {
                is List<*> ->
                    quoteInLogicalExpression(encodeOverlapAsArray(value), isInLogicalExpression)
                is Pair<*, *> ->
                    quoteInLogicalExpression(encodeOverlapAsRange(value), isInLogicalExpression)
                else -> encodeScalar(value, isInLogicalExpression)
            }
        FilterOperator.SL,
        FilterOperator.SR,
        FilterOperator.NXL,
        FilterOperator.NXR,
        FilterOperator.ADJ ->
            when (value) {
                is Pair<*, *> -> quoteInLogicalExpression(encodeAsRange(value), isInLogicalExpression)
                is List<*> -> quoteInLogicalExpression(encodeAsRange(value), isInLogicalExpression)
                else -> encodeScalar(value, isInLogicalExpression)
            }
    }

private fun encodeScalar(value: Any?, isInLogicalExpression: Boolean): String =
    if (isInLogicalExpression) escapeValue(value) else value.toString()

private fun encodeAsList(values: List<*>): String =
    values.joinToString(",", prefix = "(", postfix = ")") { escapeValue(it) }

private fun encodeAsRange(range: Pair<*, *>): String =
    "(${escapeRangeBound(range.first)},${escapeRangeBound(range.second)})"

private fun encodeAsRange(range: List<*>): String =
    "(${escapeRangeBound(range[0])},${escapeRangeBound(range[1])})"

private fun encodeOverlapAsRange(range: Pair<*, *>): String =
    "[${escapeRangeBound(range.first)},${escapeRangeBound(range.second)}]"

private fun encodeOverlapAsArray(values: List<*>): String =
    values.joinToString(",", prefix = "{", postfix = "}") { escapeArrayLiteralElement(it) }

private val quotedCharacters = setOf(',', '.', ':', '(', ')', '"', '\\', '{', '}')

private val quotedRangeBoundCharacters = setOf('(', ')', '[', ']', ',', '"', '\\')

@PublishedApi
internal fun quoteValue(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

/**
 * Inside `or`/`and` PostgREST ends an unquoted value at the first `,` or `)`, so composite values have to be quoted as a whole.
 */
internal fun quoteInLogicalExpression(value: String, isInLogicalExpression: Boolean): String =
    if (isInLogicalExpression) quoteValue(value) else value

internal fun escapeValue(value: Any?): String {
    val asString = value.toString()
    val needsQuoting = asString.isEmpty() ||
        asString.first().isWhitespace() || asString.last().isWhitespace() ||
        asString.any { it in quotedCharacters }
    return if (needsQuoting) quoteValue(asString) else asString
}

private fun escapeRangeBound(value: Any?): String {
    if (value == null) return ""
    val asString = value.toString()
    val needsQuoting = asString.isEmpty() || asString.any { it in quotedRangeBoundCharacters }
    return if (needsQuoting) quoteValue(asString) else asString
}

internal fun escapeArrayLiteralElement(value: Any?): String {
    if (value == null) return "NULL"
    val asString = value.toString()
    val needsQuoting = asString.isEmpty() ||
        asString.equals("NULL", ignoreCase = true) ||
        asString.first().isWhitespace() || asString.last().isWhitespace() ||
        asString.any { it == ',' || it == '{' || it == '}' || it == '"' || it == '\\' }
    return if (needsQuoting) quoteValue(asString) else asString
}
