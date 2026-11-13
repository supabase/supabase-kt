package io.supabase.auth

/**
 * Used to mark Postgrest filter DSL functions
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@DslMarker
annotation class PostgrestFilterDSL