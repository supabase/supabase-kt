@file:Suppress("UndocumentedPublicProperty", "ConstructorParameterNaming")
package io.supabase.postgrest.query

import io.supabase.annotations.SupabaseExperimental
import io.supabase.auth.PostgrestFilterDSL
import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.util.appendAll
import kotlinx.serialization.json.JsonElement
import kotlin.js.JsName

/**
 * A builder for Postgrest requests.
 */
@PostgrestFilterDSL
abstract class PostgrestRequestBuilder(
    defaultSchema: String,
    @PublishedApi internal val propertyConversionMethod: PropertyConversionMethod
) {

    /**
     * The [Count] algorithm to use to count rows in the table or view.
     */
    var count: Count? = null
        private set

    /**
     * The [Returning] option to use.
     */
    var returning: Returning = Returning.Minimal
        private set

    /**
     * The database schema
     */
    var schema: String = defaultSchema

    /**
     * The HTTP method to use.
     */
    internal var httpMethod: HttpMethod = HttpMethod.Get

    private var shouldStripNulls: Boolean = false
    private var dryRun: Boolean = false
    internal var acceptHeader: AcceptHeader = AcceptHeader.Json
    private var explainData: ExplainData? = null
    @PublishedApi internal var body: JsonElement? = null

    @SupabaseExperimental val params: MutableMap<String, List<String>> = mutableMapOf()
    @SupabaseExperimental val headers: HeadersBuilder = HeadersBuilder()


    /**
     * Whether to retry this request on transient errors (network errors, HTTP 503/520).
     * Only applies to idempotent requests (GET, HEAD) — non-idempotent requests are never retried.
     * Set to `false` to disable retries for this specific request.
     */
    var retry: Boolean = true
        private set

    /**
     * Disables automatic retries for this request.
     */
    fun noRetry() {
        this.retry = false
    }

    /**
     * Strip null values from the response data. Properties with `null` values
     * will be omitted from the returned JSON objects.
     *
     * Requires PostgREST 11.2.0+.
     *
     */
    fun stripNulls() {
        require(acceptHeader != AcceptHeader.CSV) {
            "stripNulls() cannot be used with csv()"
        }
        this.shouldStripNulls = true
    }

    /**
     * Setting [count] allows to use [PostgrestResult.countOrNull] to get the total amount of items in the database.
     * @param count algorithm to use to count rows in the table or view.
     */
    fun count(count: Count) {
        this.count = count
    }

    /**
     * Return `data` after the query has been executed.
     * @param columns The columns to return
     */
    fun select(columns: Columns = Columns.ALL) {
        this.returning = Returning.Representation(columns).also {
            params["select"] = listOf(it.columns.value)
        }
    }

    /**
     * Return `data` as a single object instead of an array of objects.
     *
     * Query result must be one row (e.g. using `limit(1)`), otherwise this
     * returns an error.
     */
    @JsName("singleValue")
    fun single() {
        acceptHeader = AcceptHeader.Single(false)
    }

    /**
     * Return `data` as a single object instead of an array of objects.
     *
     * Instructs PostgREST to return a single JSON object, returning `null`/blank data when no row matches.
     *
     * Like ``single()``, this sets the `application/vnd.pgrst.object+json` accept header so the
     * server enforces a single result. Unlike ``single()``, when the query does not match exactly
     * one row the resulting `PGRST116` error is not thrown — ``PostgrestResponse/value`` is `null`
     * instead.
     *
     * > Note: PostgREST returns `PGRST116` both when zero rows match and when more than one row
     * > matches. This method returns `null` for either case; use ``single()`` for the strict variant
     * > that always throws when the query does not match exactly one row.
     */
    @JsName("maybeSingleValue")
    fun maybeSingle() {
        acceptHeader = AcceptHeader.Single(true)
    }

    /**
     * Executes the query but rolls back the transaction instead of committing it.
     *
     * The mutation runs and its result (including any side effects such as triggers) is returned
     * in the response, but the transaction is rolled back afterward, so no changes are persisted.
     * This is useful for testing mutations without touching real data.
     *
     * Requires PostgREST's `db-tx-end` setting to allow client-controlled transaction rollback.
     */
    fun dryRun() {
        dryRun = true
    }

    /**
     * Orders the result by [column] in the specified [order].
     * @param column The column to order by
     * @param order The order to use
     * @param nullsFirst If true, null values will be ordered first
     * @param referencedTable If the column is from a foreign table, specify the table name here
     */
    fun order(column: String, order: Order, nullsFirst: Boolean = false, referencedTable: String? = null) {
        val key = if (referencedTable == null) "order" else "$referencedTable.order"
        val orderEntry = params[key]?.firstOrNull()
        val existingOrder = if (orderEntry == null) "" else "$orderEntry,"
        val newOrder = "$existingOrder${column}.${order.value}.${if (nullsFirst) "nullsfirst" else "nullslast"}"
        params[key] = listOf(newOrder)
    }

    /**
     * Limits the result to [count] rows
     * @param count The amount of rows to return
     * @param referencedTable If the column is from a foreign table, specify the table name here
     */
    fun limit(count: Long, referencedTable: String? = null) {
        val key = if (referencedTable == null) "limit" else "$referencedTable.limit"
        params[key] = listOf(count.toString())
    }

    /**
     * Limits the result to rows from [from] to [to]
     * @param from The first row to return
     * @param to The last row to return
     * @param referencedTable If the column is from a foreign table, specify the table name here
     */
    fun range(from: Long, to: Long, referencedTable: String? = null) {
        val keyOffset = if (referencedTable == null) "offset" else "$referencedTable.offset"
        val keyLimit = if (referencedTable == null) "limit" else "$referencedTable.limit"

        params[keyOffset] = listOf(from.toString())
        params[keyLimit] = listOf((to - from + 1).toString())
    }

    /**
     * Limits the result to rows from [range.first] to [range.last]
     * @param range The range of rows to return
     * @param referencedTable If the column is from a foreign table, specify the table name here
     */
    fun range(range: LongRange, referencedTable: String? = null) = range(range.first, range.last, referencedTable)

    /**
     * Return `data` as an object in [GeoJSON](https://geojson.org) format.
     */
    fun geojson() {
        acceptHeader = AcceptHeader.GeoJson
    }

    /**
     * Return `data` as a string in CSV format.
     */
    fun csv() {
        acceptHeader = AcceptHeader.CSV
    }

    /**
     * Return `data` as the EXPLAIN plan for the query.
     *
     * @param analyze - If `true`, the query will be executed and the
     * actual run time will be returned
     * @param verbose - If `true`, the query identifier will be returned
     * and `data` will include the output columns of the query
     * @param settings - If `true`, include information on configuration
     * parameters that affect query planning
     * @param buffers - If `true`, include information on buffer usage
     * @param wal - If `true`, include information on WAL record generation
     * @param format - The format of the output, can be `"text"` (default)
     * or `"json"`
     */
    @Suppress("LongParameterList")
    fun explain(
        analyze: Boolean = false,
        verbose: Boolean = false,
        settings: Boolean = false,
        buffers: Boolean = false,
        wal: Boolean = false,
        format: String = "text",
    ) {
        val options = buildList {
            if (analyze) add("analyze")
            if (verbose) add("verbose")
            if (settings) add("settings")
            if (buffers) add("buffers")
            if (wal) add("wal")
        }.joinToString("|")
        explainData = ExplainData(options, format)
    }

    /**
     * Adds a filter to the postgrest request.
     * @param block The filter block
     */
    inline fun filter(block: @PostgrestFilterDSL PostgrestFilterBuilder.() -> Unit) {
        val filter = PostgrestFilterBuilder(propertyConversionMethod, params)
        filter.block()
    }

    protected fun withReturning() = listOf("return=${returning.identifier}")

    internal open fun customPrefer(): List<String> = listOf()

    internal fun buildPrefer() = buildSet {
        if (count != null) add("count=${count!!.identifier}")
        if (dryRun) add("tx=rollback")
        addAll(customPrefer())
    }

    internal fun HttpRequestBuilder.apply() {

        //
        this.method = httpMethod
        contentType(ContentType.Application.Json)
        this@PostgrestRequestBuilder.body?.let { setBody(it) }

        // Schema
        if (schema.isNotBlank()) {
            when (httpMethod) {
                HttpMethod.Get, HttpMethod.Head -> header("Accept-Profile", schema)
                else -> header("Content-Profile", schema)
            }
        }

        val mediaType = when(acceptHeader) {
            AcceptHeader.CSV -> AcceptHeader.CSV.headerValue()
            AcceptHeader.GeoJson -> AcceptHeader.GeoJson.headerValue()
            AcceptHeader.Json -> AcceptHeader.Json.headerValue(shouldStripNulls)
            is AcceptHeader.Single -> AcceptHeader.Single.headerValue(shouldStripNulls)
        }

        // Accept header
        header(
            HttpHeaders.Accept,
            if(explainData != null) explainData!!.headerValue(mediaType) else mediaType
        )

        header(PostgrestQueryBuilder.HEADER_PREFER, buildPrefer().joinToString(","))

        // Url params & headers
        params.forEach { (name, values) -> url.parameters.appendAll(name, values) }
        headers.appendAll(this@PostgrestRequestBuilder.headers)
    }

}

