package io.supabase.logging

/**
 * An interface for processing logs
 */
interface SupabaseLoggingProcessor {

    /**
     * Returns whether log messages at the given [level] are enabled.
     * @param level The log level
     */
    fun isEnabled(level: LogLevel): Boolean

    /**
     * Log a message with the given [level], [tag] and [message]. An optional [throwable] can be provided.
     * @param level The log level
     * @param tag The logging tag
     * @param throwable An optional throwable
     * @param message The message to log
     */
    fun processLog(level: LogLevel, tag: String, throwable: Throwable? = null, message: String)

}

/**
 * Log a message with the given [level], [tag] and [message]. Only invokes the [message] lambda, if the level [isEnabled][SupabaseLoggingProcessor.isEnabled]. An optional [throwable] can be provided.
 * @param level The log level
 * @param tag The logging tag
 * @param throwable An optional throwable
 * @param message The message to log
 */
inline fun SupabaseLoggingProcessor.log(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) {
    if(isEnabled(level)) {
        processLog(level, tag, throwable, message())
    }
}