@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)
package io.supabase.postgrest.query.filter

/**
 * Used to search rows using a full text search. See [Postgrest](https://postgrest.org/en/stable/api.html#full-text-search) for more information
 */
enum class TextSearchType(val identifier: String) {
    NONE(""),
    PLAINTO("pl"),
    PHRASETO("ph"),
    WEBSEARCH("w")
}