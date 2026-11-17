package io.supabase.postgrest.executor

import io.supabase.postgrest.Postgrest
import io.supabase.postgrest.query.PostgrestRequestBuilder
import io.supabase.postgrest.result.PostgrestResult

/**
 * Request executor interface
 *
 */
sealed interface RequestExecutor {

    /**
     * Execute given PostgrestRequest
     *
     * @param path [String]
     * @param request [PostgrestRequestBuilder]
     * @return [PostgrestResult]
     */
    suspend fun execute(postgrest: Postgrest, path: String, request: PostgrestRequestBuilder): PostgrestResult
}