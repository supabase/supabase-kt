package io.supabase.storage.resumable

import kotlin.jvm.JvmInline

/**
 * Represents a fingerprint of an upload url. This is used to identify an upload url in the cache. Use [Fingerprint.invoke] to create a fingerprint
 * @param value The fingerprint value
 */
@JvmInline
value class Fingerprint private constructor(val value: String) {

    /**
     * The source of the file upload
     */
    val source get() = value.substringBeforeLast(FINGERPRINT_SEPARATOR)

    /**
     * The size of the data
     */
    val size get() = value.substringAfterLast(FINGERPRINT_SEPARATOR).toLong()

    companion object {

        /**
         * The amount of parts a fingerprint consists of (separator is "[FINGERPRINT_SEPARATOR]")
         */
        const val FINGERPRINT_PARTS = 2

        /**
         * The separator between the parts of the fingerprint
         */
        const val FINGERPRINT_SEPARATOR = "::"

        /**
         * Creates a fingerprint from the [source] and the [size] of the file
         */
        operator fun invoke(source: String, size: Long) = Fingerprint("$source$FINGERPRINT_SEPARATOR$size")

        /**
         * Creates a fingerprint from the [value]. Returns null if the [value] is not a valid fingerprint
         */
        operator fun invoke(value: String): Fingerprint? {
            if(!value.contains(FINGERPRINT_SEPARATOR)) return null
            if(value.substringAfterLast(FINGERPRINT_SEPARATOR).toLongOrNull() == null) return null
            return Fingerprint(value)
        }

    }

}