package io.supabase.storage

import io.supabase.SupabaseSerializer
import io.supabase.encodeToJsonElement
import io.supabase.network.HttpRequestOverride
import io.ktor.http.ContentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Builder for uploading files with additional options
 * @param serializer The serializer to use for encoding the metadata
 * @param upsert Whether to update the file if it already exists
 * @param userMetadata The user metadata to upload with the file
 * @param contentType The content type of the file. If null, the content type will be inferred from the file extension
 */
class UploadOptionBuilder(
    @PublishedApi internal val serializer: SupabaseSerializer,
    var upsert: Boolean = false,
    var userMetadata: JsonObject? = null,
    var contentType: ContentType? = null,
    internal val httpRequestOverrides: MutableList<HttpRequestOverride> = mutableListOf()
) {

    /**
     * Adds an [HttpRequestOverride] to the upload request
     * @param override The override to add
     */
    fun httpOverride(override: HttpRequestOverride) {
        httpRequestOverrides.add(override)
    }

    /**
     * Sets the user metadata to upload with the file
     * @param data The data to upload. Must be serializable by the [serializer]
     */
    inline fun <reified T : Any> userMetadata(data: T) {
        userMetadata = serializer.encodeToJsonElement(data).jsonObject
    }

    /**
     * Sets the user metadata to upload with the file
     * @param builder The builder for the metadata
     */
    inline fun userMetadata(builder: JsonObjectBuilder.() -> Unit) {
        userMetadata = buildJsonObject(builder)
    }

}
