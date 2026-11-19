@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)
package io.supabase.postgrest.query.filter

/**
 * Represents a single filter operation
 */
enum class FilterOperator(val identifier: String) {
    EQ("eq"),
    NEQ("neq"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    LIKE("like"),
    MATCH("match"),
    ILIKE("ilike"),
    IMATCH("imatch"),
    IS("is"),
    IN("in"),
    CS("cs"),
    CD("cd"),
    SL("sl"),
    SR("sr"),
    NXL("nxl"),
    NXR("nxr"),
    ADJ("adj"),
    OV("ov"),
    FTS("fts"),
    PLFTS("plfts"),
    PHFTS("phfts"),
    WFTS("wfts"),
    ISDISTINCT("isdistinct");
}