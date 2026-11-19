package io.supabase.realtime

import io.supabase.SupabaseSerializer
import io.supabase.decode
import io.supabase.plugins.SerializableData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/**
 * Contains information about a column
 * @property name the name of the column
 * @property type the type of the column
 */
@Serializable
data class Column(val name: String, val type: String)

/**
 * Represents a postgres action, containing a record.
 */
interface HasRecord: SerializableData {

    /**
     * The new record, if the action has one
     */
    val record: JsonObject
}

/**
 * Represents a postgres action, containing an old record.
 */
interface HasOldRecord: SerializableData {

    /**
     * The old record, if the action has one
     */
    val oldRecord: JsonObject
}

/**
 * Represents a postgres action
 */
sealed interface PostgresAction: SerializableData {

    /**
     * Contains data of the row's columns
     */
    val columns: List<Column>

    /**
     * The time when the action was committed
     */
    val commitTimestamp: Instant

    /**
     * Represents a postgres insert action
     */
    data class Insert(
        override val record: JsonObject,
        override val columns: List<Column>,
        override val commitTimestamp: Instant,
        override val serializer: SupabaseSerializer,
    ) : PostgresAction, HasRecord

    /**
     * Represents a postgres update action
     */
    data class Update(
        override val record: JsonObject,
        override val oldRecord: JsonObject,
        override val columns: List<Column>,
        override val commitTimestamp: Instant,
        override val serializer: SupabaseSerializer,
    ) : PostgresAction, HasRecord, HasOldRecord

    /**
     * Represents a postgres delete action
     */
    data class Delete(
        override val oldRecord: JsonObject,
        override val columns: List<Column>,
        override val commitTimestamp: Instant,
        override val serializer: SupabaseSerializer,
    ) : PostgresAction, HasOldRecord

    /**
     * Represents a postgres select action
     */
    data class Select(
        override val record: JsonObject,
        override val columns: List<Column>,
        override val commitTimestamp: Instant,
        override val serializer: SupabaseSerializer,
    ) : PostgresAction, HasRecord

}

/**
 * Decodes [HasRecord.record] as [T] and returns it or returns null when it cannot be decoded as [T]
 */
inline fun <reified T : Any> HasRecord.decodeRecordOrNull(): T? {
    return try {
        serializer.decode(record.toString())
    } catch (e: Exception) {
        null
    }
}

/**
 * Decodes [HasOldRecord.oldRecord] as [T] and returns it or returns null when it cannot be decoded as [T]
 */
inline fun <reified T : Any> HasOldRecord.decodeOldRecordOrNull(): T? {
    return try {
        serializer.decode(oldRecord.toString())
    } catch (e: Exception) {
        null
    }
}

/**
 * Decodes [HasRecord.record] as [T] and returns it
 */
inline fun <reified T : Any> HasRecord.decodeRecord() = serializer.decode<T>(record.toString())

/**
 * Decodes [HasOldRecord.oldRecord] as [T] and returns it
 */
inline fun <reified T : Any> HasOldRecord.decodeOldRecord() = serializer.decode<T>(oldRecord.toString())