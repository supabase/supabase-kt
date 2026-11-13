@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)
package io.supabase.realtime.postgres

import io.supabase.postgrest.query.filter.FilterOperator

enum class RealtimePostgresChangesFilterOperator {
    EQ, NEQ, LT, LTE, GT, GTE, IN, LIKE, ILIKE, IS, MATCH, IMATCH, ISDISTINCT
}

internal fun RealtimePostgresChangesFilterOperator.toPostgrestOperator() = when(this) {
    RealtimePostgresChangesFilterOperator.EQ -> FilterOperator.EQ
    RealtimePostgresChangesFilterOperator.NEQ -> FilterOperator.NEQ
    RealtimePostgresChangesFilterOperator.LT -> FilterOperator.LT
    RealtimePostgresChangesFilterOperator.LTE -> FilterOperator.LTE
    RealtimePostgresChangesFilterOperator.GT -> FilterOperator.GT
    RealtimePostgresChangesFilterOperator.GTE -> FilterOperator.GTE
    RealtimePostgresChangesFilterOperator.IN -> FilterOperator.IN
    RealtimePostgresChangesFilterOperator.LIKE -> FilterOperator.LIKE
    RealtimePostgresChangesFilterOperator.ILIKE -> FilterOperator.ILIKE
    RealtimePostgresChangesFilterOperator.IS -> FilterOperator.IS
    RealtimePostgresChangesFilterOperator.MATCH -> FilterOperator.MATCH
    RealtimePostgresChangesFilterOperator.IMATCH -> FilterOperator.IMATCH
    RealtimePostgresChangesFilterOperator.ISDISTINCT -> FilterOperator.ISDISTINCT
}