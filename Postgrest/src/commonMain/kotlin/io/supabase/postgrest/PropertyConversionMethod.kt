package io.supabase.postgrest

import io.supabase.CurrentPlatformTarget
import io.supabase.PlatformTarget
import kotlin.reflect.KProperty1

/**
 * A method to convert a property name to a column name.
 */
fun interface PropertyConversionMethod {

    /**
     * Converts a property name to a column name.
     */
    operator fun invoke(property: KProperty1<*, *>): String

    companion object {

        /**
         * Converts a property name to a column name using the [SerialName] annotation.
         */
        val SERIAL_NAME = PropertyConversionMethod {
            if(CurrentPlatformTarget !in listOf(PlatformTarget.JVM, PlatformTarget.ANDROID)) error("SerialName PropertyConversionMethod is only available on the JVM and ANDROID due to limited reflection on other targets. Use CAMEL_CASE_TO_SNAKE_CASE instead.")
            getSerialName(it)
        }

        /**
         * Converts a property name to a column name by converting camel case to snake case.
         */
        val CAMEL_CASE_TO_SNAKE_CASE = PropertyConversionMethod { it.name.camelToSnakeCase() }

        /**
         * Uses the property name as the column name.
         */
        val NONE = PropertyConversionMethod { it.name }
    }

}