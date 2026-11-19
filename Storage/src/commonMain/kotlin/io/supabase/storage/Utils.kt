package io.supabase.storage

import io.ktor.http.encodeURLPath
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

internal fun JsonObjectBuilder.putImageTransformation(transformation: ImageTransformation) {
    transformation.width?.let { put("width", it) }
    transformation.height?.let { put("height", it) }
    transformation.resize?.let { put("resize", it.name.lowercase()) }
    transformation.quality?.let { put("quality", it) }
    transformation.format?.let { put("format", it) }
}

internal fun storagePath(bucketId: String, path: String? = null) =
    bucketId.encodeURLPath() + if (path != null) {
        "/${path.split("/").joinToString("/") { it.encodeURLPath() }}"
    } else {
        ""
    }