package io.supabase.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

/**
 * A [SupabaseLoggingProcessor] implementation using Kermit.
 * @param level The minimum log level for this logger.
 */
internal class KermitLoggingProcessor(
    level: LogLevel,
) : SupabaseLoggingProcessor {

    private val logger: Logger = Logger(
        config = loggerConfigInit(platformLogWriter(), minSeverity = level.toSeverity())
    )

    override fun isEnabled(level: LogLevel): Boolean {
        return logger.config.minSeverity <= level.toSeverity()
    }

    override fun processLog(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        logger.processLog(level.toSeverity(), tag, throwable, message)
    }

    private fun LogLevel.toSeverity() = when (this) {
        LogLevel.DEBUG -> Severity.Debug
        LogLevel.INFO -> Severity.Info
        LogLevel.WARNING -> Severity.Warn
        LogLevel.ERROR -> Severity.Error
        LogLevel.NONE -> Severity.Assert
    }

}