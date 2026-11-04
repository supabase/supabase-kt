package io.supabase.auth

import io.supabase.SupabaseClient
import io.supabase.annotations.SupabaseExperimental

/**
 * A [UrlLauncher] is used to open a URL in the system browser.
 */
@SupabaseExperimental
fun interface UrlLauncher {

    /**
     * Open the given URL in the system browser.
     * @param url The URL to open.
     */
    suspend fun openUrl(supabase: SupabaseClient, url: String)

    companion object {

        /**
         * Default implementation of [UrlLauncher] that opens the URL in the system browser.
         */
        val DEFAULT = UrlLauncher { supabase, url ->
            supabase.openExternalUrl(url)
        }

    }

}