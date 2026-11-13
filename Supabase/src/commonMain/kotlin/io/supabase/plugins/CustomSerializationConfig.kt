package io.supabase.plugins

import io.supabase.SupabaseClientBuilder
import io.supabase.SupabaseSerializer

/**
 * A configuration for a plugin, which allows to customize the serialization
 */
interface CustomSerializationConfig {

    /**
     * The serializer used for this module. Defaults to [SupabaseClientBuilder.defaultSerializer], when null.
     */
    var serializer: SupabaseSerializer?

}