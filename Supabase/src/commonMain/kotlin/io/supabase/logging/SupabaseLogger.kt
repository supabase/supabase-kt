package io.supabase.logging

import io.supabase.SupabaseClient
import io.supabase.SupabaseLoggingProcessorFactory
import io.supabase.plugins.MainConfig

/**
 * An interface for logging in Supabase plugins.
 * @param level The minimum log level to handle for this logger. If null, [SupabaseClient.DEFAULT_LOG_LEVEL] will be used.
 * @param processor The logging processor used for the actual logging
 */
class SupabaseLogger(
    val level: LogLevel,
    val tag: String,
    processorFactory: SupabaseLoggingProcessorFactory
) {

    val processor = processorFactory(level)

    /**
     * Log a message with the given [level] and [message]. An optional [throwable] can be provided.
     * @param level The log level
     * @param throwable An optional throwable
     * @param message The message to log
     */
    fun log(level: LogLevel, throwable: Throwable? = null, message: String) = processor.log(level, tag, throwable) {
        message
    }

    /**
     * Log a message with the given [level] and [message]. An optional [throwable] can be provided.
     * @param level The log level
     * @param throwable An optional throwable
     * @param message The message to log
     */
    inline fun log(level: LogLevel, throwable: Throwable? = null, message: () -> String) = processor.log(level, tag, throwable, message)

    /**
     * Creates a new logger with the new [tag], but with the same [processor]
     * @param tag The new tag
     */
    fun withTag(tag: String) = SupabaseLogger(level, tag, { _ -> processor })

    /**
     * Creates a new logger and appends [tag] to the current tag
     * @param tag The tag to append
     */
    fun appendTag(tag: String) = withTag(this.tag + tag)

}

/**
 * Log a debug message with an optional [throwable]
 * @param throwable An optional throwable
 * @param message The message to log
 */
inline fun SupabaseLogger.d(throwable: Throwable? = null, message: () -> String) {
    log(LogLevel.DEBUG, throwable, message)
}

/**
 * Log an info message with an optional [throwable]
 * @param throwable An optional throwable
 * @param message The message to log
 */
inline fun SupabaseLogger.i(throwable: Throwable? = null, message: () -> String) {
    log(LogLevel.INFO, throwable, message)
}

/**
 * Log a warning message with an optional [throwable]
 * @param throwable An optional throwable
 * @param message The message to log
 */
inline fun SupabaseLogger.w(throwable: Throwable? = null, message: () -> String) {
    log(LogLevel.WARNING, throwable, message)
}

/**
 * Log an error message with an optional [throwable]
 * @param throwable An optional throwable
 * @param message The message to log
 */
inline fun SupabaseLogger.e(throwable: Throwable? = null, message: () -> String) {
    log(LogLevel.ERROR, throwable, message)
}

fun SupabaseClient.createLogger(
    tag: String,
    logLevel: LogLevel?,
    loggingFactory: SupabaseLoggingProcessorFactory?
) = SupabaseLogger(logLevel ?: logger.level, tag, loggingFactory ?: config.loggingConfig.defaultLoggingFactory)

fun SupabaseClient.createLogger(
    tag: String,
    config: MainConfig
) = createLogger(tag, config.logLevel,config.loggingFactory)