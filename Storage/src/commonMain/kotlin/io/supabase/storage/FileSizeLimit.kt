package io.supabase.storage

import kotlin.jvm.JvmInline

/**
 * A file size limit for buckets
 * @param value The value of the limit (e.g. 10mb)
 */
@JvmInline
value class FileSizeLimit internal constructor(val value: String)