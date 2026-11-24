package io.supabase.auth

import io.supabase.annotations.SupabaseInternal

/**
 * The prefix of the keys (or file names) the default session and code verifier storage use for a project.
 */
@SupabaseInternal
fun createDefaultSettingsKey(supabaseUrl: String) =
    "sb-${supabaseUrl.removeSuffix("/").replace('/', '-').replace('.', '-')}"
