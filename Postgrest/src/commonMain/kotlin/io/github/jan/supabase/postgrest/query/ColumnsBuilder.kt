package io.github.jan.supabase.postgrest.query

internal object Aggregates {
    const val AVG = "avg()"
    const val COUNT = "count()"
    const val MAX = "max()"
    const val MIN = "min()"
    const val SUM = "sum()"
}

/**
 * Type-safe builder for selecting columns
 */
open class BasicColumnsBuilder {

    internal val columns: MutableList<String> = mutableListOf<String>()

    /**
     * Selects the given [columns].
     * - To rename a column, use the [withAlias] infix function.
     * - To use a function on a column, use the [withFunction] infix function.
     * - To specify a type for a column/cast a column value, use the [withType] infix function.
     *
     * Example:
     * ```kotlin
     * named("name" withAlias "my_name", "id" withType "text") // name AS my_name, id::text
     * ```
     * @param columns The columns to select
     */
    fun named(vararg columns: String) {
        this.columns.addAll(columns)
    }

    /**
     * Selects all/the remaining columns
     */
    fun all() {
        columns.add("*")
    }

    /**
     * Selects a JSON column
     *
     * For example to select the key `key` from the JSON column in `json_data`:
     *
     * ```json
     * {
     *    "key": "value",
     *    "array": [{
     *        "key": "value"
     *    }]
     * }
     * ```
     *
     * ```kotlin
     * json("json_data", "array", "0", "key", returnAsText = true) // jsonData->array->0->>key
     * ```
     *
     * @param column The column to select
     * @param path The path to the JSON key
     * @param returnAsText Whether to return the JSON key as text
     */
    fun json(column: String, vararg path: String, returnAsText: Boolean = false) {
        val operator = if(returnAsText) "->>" else "->"
        val formattedPath = if(path.size > 1) path.dropLast(1).joinToString("->", prefix = "->") else ""
        val key = path.last()
        columns.add("$column$formattedPath$operator$key")
    }

    /**
     * Selects a foreign column
     * @param name The name of the foreign column or the table name
     * @param columnsBuilder The columns to select from the foreign column
     */
    fun foreign(name: String, columnsBuilder: ForeignColumnsBuilder.() -> Unit = {}) {
        val foreignColumns = ForeignColumnsBuilder().apply(columnsBuilder)
        val spread = if(foreignColumns.spread) "..." else ""
        val key = if(foreignColumns.key != null) "!${foreignColumns.key}" else ""
        columns.add("$spread$name$key(${foreignColumns.build()})")
    }

    /**
     * Renames a column to the given [alias]
     * @param alias The alias to rename the column to
     */
    infix fun String.withAlias(alias: String) = "$alias:$this"

    /**
     * Applies a function to the column
     * @param name The name of the function
     */
    infix fun String.withFunction(name: String) = "$this.$name"

    /**
     * Casts a column to the given [type]
     * @param type The type to cast the column to
     */
    infix fun String.withType(type: String) = "$this::$type"

    /**
     * Applies the `avg()` function to the column
     */
    fun avg() = Aggregates.AVG

    /**
     * Applies the `count()` function to the column
     */
    fun count() = Aggregates.COUNT

    /**
     * Applies the `max()` function to the column
     */
    fun max() = Aggregates.MAX

    /**
     * Applies the `min()` function to the column
     */
    fun min() = Aggregates.MIN

    /**
     * Applies the `sum()` function to the column
     */
    fun sum() = Aggregates.SUM

    @PublishedApi
    internal fun build() = columns.joinToString(",")

}

/**
 * Type-safe builder for selecting columns
 */
class ForeignColumnsBuilder: BasicColumnsBuilder() {

    /**
     * Whether to spread the foreign columns in the response
     */
    var spread = false

    /**
     * The key to use for the foreign column when having multiple foreign columns
     */
    var key: String? = null

}

internal fun String.clean(): String {
    var quoted = false
    val regex = Regex("\\s")
    return this.map {
        if (it == '"') {
            quoted = !quoted
        }
        if (regex.matches(it.toString()) && !quoted) {
            ""
        } else {
            it
        }
    }.joinToString("")
}