package io.supabase.realtime.annotations

/**
 * Used to mark Channel DSL functions
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@DslMarker
annotation class ChannelDsl
