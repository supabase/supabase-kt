package io.supabase.postgrest.executor

import io.supabase.postgrest.Postgrest
import io.supabase.postgrest.result.PostgrestResult
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

internal suspend fun HttpResponse.asPostgrestResult(postgrest: Postgrest): PostgrestResult =
    PostgrestResult(bodyAsText(), headers, postgrest)