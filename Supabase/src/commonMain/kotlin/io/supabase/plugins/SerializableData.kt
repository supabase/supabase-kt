package io.supabase.plugins

import io.supabase.SupabaseSerializer
import io.supabase.annotations.SupabaseInternal

/**
 * An interface for data that can be serialized with [SupabaseSerializer]
 */
interface SerializableData {

    /**
     * The serializer used to serialize this data
     */
    @SupabaseInternal
    val serializer: SupabaseSerializer

}