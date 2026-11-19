package io.supabase.postgrest.query.request

import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.PostgrestQueryBuilder
import io.ktor.http.HttpMethod

/**
 * Request builder for [PostgrestQueryBuilder.upsert]
 */
class UpsertRequestBuilder(defaultSchema: String, propertyConversionMethod: PropertyConversionMethod): InsertRequestBuilder(defaultSchema, propertyConversionMethod){

    /**
     * Comma-separated UNIQUE column(s) to specify how
     * duplicate rows are determined. Two rows are duplicates if all the
     * `onConflict` columns are equal.
     */
    var onConflict: String? = null
        set(value) {
            value?.let {
                params["on_conflict"] = listOf(value)
            }
            field = value
        }

    /**
     * If `true`, duplicate rows are ignored. If `false`, duplicate rows are merged with existing rows.
     */
    var ignoreDuplicates: Boolean = false

    init {
        httpMethod = HttpMethod.Post
    }

    override fun customPrefer(): List<String> {
        return buildPrefer(upsert = true, ignoreDuplicates = ignoreDuplicates)
    }

}