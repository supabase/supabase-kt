package io.supabase.realtime

@PublishedApi
internal object RealtimeTopic {

    const val PREFIX = "realtime"

    fun withChannelId(channelId: String): String {
        return "$PREFIX:$channelId"
    }

}