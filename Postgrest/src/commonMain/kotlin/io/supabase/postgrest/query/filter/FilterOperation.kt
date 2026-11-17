@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)

package io.supabase.postgrest.query.filter

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
        FilterOperator.PLFTS ->
            if (isInLogicalExpression)
                escapeValue(value)
            else
                value.toString()
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
                    encodeOverlapAsArray(value)
                is Pair<*, *> ->
                    encodeOverlapAsRange(value)
                else ->
                    if (isInLogicalExpression)
                        escapeValue(value)
                    else
                        value.toString()
            }
        FilterOperator.SL,
        FilterOperator.SR,
        FilterOperator.NXL,
        FilterOperator.NXR,
        FilterOperator.ADJ ->
            when (value) {
                is Pair<*, *> -> encodeAsRange(value)
                is List<*> -> encodeAsRange(value)
                else -> escapeValue(value)
            }
    }

private fun encodeAsList(values: List<*>): String =
    values.joinToString(",", prefix = "(", postfix = ")") { escapeValue(it) }

private fun encodeAsRange(range: Pair<*, *>): String =
    "(${range.first},${range.second})"

private fun encodeAsRange(range: List<*>): String =
    "(${range[0]},${range[1]})"

private fun encodeOverlapAsRange(range: Pair<*, *>): String =
    "[${range.first},${range.second}]"

private fun encodeOverlapAsArray(values: List<*>): String =
    values.joinToString(",", prefix = "{", postfix = "}") { escapeArrayLiteralElement(it) }

private val quotedCharacters = listOf(",", ".", ":", "(", ")")

internal fun escapeValue(value: Any?): String {
    val asString = value.toString()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return if (quotedCharacters.any { asString.contains(it) }) {
        "\"$asString\""
    } else {
        asString
    }
}

internal fun escapeArrayLiteralElement(value: Any?): String {
    if (value == null) return "NULL"
    val asString = value.toString()
    val needsQuoting = asString.isEmpty() ||
        asString.equals("NULL", ignoreCase = true) ||
        asString.first().isWhitespace() || asString.last().isWhitespace() ||
        asString.any { it == ',' || it == '{' || it == '}' || it == '"' || it == '\\' }
    if (!needsQuoting) return asString
    val escaped = asString
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}
