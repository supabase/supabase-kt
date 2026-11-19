package io.supabase.plugins

import io.supabase.SupabaseClientBuilder
import io.supabase.SupabaseSerializer

/**
 * A plugin, which allows to customize the serialization
 */
interface CustomSerializationPlugin {

    /**
     * The serializer used for this module. Defaults to [SupabaseClientBuilder.defaultSerializer], when null.
     */
    val serializer: SupabaseSerializer

}