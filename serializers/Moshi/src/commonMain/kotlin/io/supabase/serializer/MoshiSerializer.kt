package io.supabase.serializer

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.supabase.SupabaseSerializer
import kotlin.reflect.KType

/**
 * A [SupabaseSerializer] that uses moshi
 */
class MoshiSerializer(
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) : SupabaseSerializer {

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> encode(type: KType, value: T): String = moshi.adapter<T>(type).serializeNulls().toJson(value)

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> decode(type: KType, value: String): T {
        val adapter = moshi.adapter<T>(type)
        return adapter.fromJson(value) as T
    }

}