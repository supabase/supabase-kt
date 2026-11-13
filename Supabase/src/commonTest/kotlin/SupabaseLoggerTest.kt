import io.supabase.logging.LogLevel
import io.supabase.logging.SupabaseLogger
import io.supabase.logging.SupabaseLoggingProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseLoggerTest {

    class MockLoggingProcessor(
        private val logLevel: LogLevel,
        private val writeChannel: SendChannel<String>,
        private val coroutineScope: CoroutineScope
    ) : SupabaseLoggingProcessor {

        override fun isEnabled(level: LogLevel): Boolean {
            return level >= logLevel
        }

        override fun processLog(
            level: LogLevel,
            tag: String,
            throwable: Throwable?,
            message: String
        ) {
           coroutineScope.launch {
               writeChannel.send(level.name + tag + message)
           }
        }
    }

    @Test
    fun testLogging() = runTest {
        val channel = Channel<String>()
        val logger = SupabaseLogger(LogLevel.DEBUG, "MyTag") { MockLoggingProcessor(it, channel, backgroundScope) }
        logger.log(LogLevel.DEBUG, null, "Test")
        advanceUntilIdle()
        assertEquals("DEBUGMyTagTest", channel.receive())
    }

    @Test
    fun testLogLevelFiltering() = runTest {
        val channel = Channel<String>(capacity = 10)
        // Set processor to INFO
        val logger = SupabaseLogger(LogLevel.INFO, "Tag") { MockLoggingProcessor(it, channel, backgroundScope) }

        // 1. Log at DEBUG (should be ignored if INFO is the threshold)
        logger.log(LogLevel.DEBUG, null, "Lower priority")

        // 2. Log at ERROR (should be allowed)
        logger.log(LogLevel.ERROR, null, "Higher priority")

        advanceUntilIdle()

        // Check that we only got the ERROR message, not the DEBUG one
        assertEquals("ERRORTagHigher priority", channel.receive())
        assertTrue(channel.isEmpty, "Channel should have no more messages")
    }

    @Test
    fun testRapidFireLoggingOrder() = runTest {
        val channel = Channel<String>(capacity = 100)
        val logger = SupabaseLogger(LogLevel.DEBUG, "Tag", { MockLoggingProcessor(it, channel, backgroundScope)} )

        val count = 50
        repeat(count) { i ->
            logger.log(LogLevel.DEBUG, null, "Msg $i")
        }

        advanceUntilIdle()

        repeat(count) { i ->
            assertEquals("DEBUGTagMsg $i", channel.receive())
        }
    }

}