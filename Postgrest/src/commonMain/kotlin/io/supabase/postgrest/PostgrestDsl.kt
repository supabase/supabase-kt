package io.supabase.postgrest

import io.supabase.annotations.SupabaseInternal

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@SupabaseInternal
annotation class PostgrestDsl
