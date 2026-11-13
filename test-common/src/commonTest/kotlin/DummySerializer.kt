import io.supabase.SupabaseSerializer
import kotlin.reflect.KType

class DummySerializer: SupabaseSerializer {
    override fun <T> encode(type: KType, value: T): String {
        TODO("Not yet implemented")
    }

    override fun <T> decode(type: KType, value: String): T {
        TODO("Not yet implemented")
    }
}