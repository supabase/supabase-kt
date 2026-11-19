import io.supabase.realtime.RealtimeTopic
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeTopicTest {

    @Test
    fun testRealtimeTopic() {
        val channelId = "channelId"
        val topic = RealtimeTopic.withChannelId(channelId)
        assertEquals("realtime:channelId", topic)
    }

    @Test
    fun testRealtimePrefix() {
        assertEquals("realtime", RealtimeTopic.PREFIX)
    }

}