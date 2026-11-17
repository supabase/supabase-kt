package io.supabase.storage

import io.ktor.utils.io.ByteWriteChannel
import kotlin.jvm.JvmInline

/**
 * Represents the status of a download
 */
sealed interface DownloadStatus {

    /**
     * Represents the progress of a download
     * @param totalBytesReceived The total bytes received
     * @param contentLength The total bytes to receive
     */
    //TODO: Replace with multi-field value class
    data class Progress(val totalBytesReceived: Long, val contentLength: Long) : DownloadStatus

    /**
     * Represents the success of a download
     */
    data object Success : DownloadStatus

    /**
     * Represents the data of a download. Only sent, when not streaming to a [ByteWriteChannel]
     * @param data The data of the download
     */
    @JvmInline
    value class ByteData(val data: ByteArray) : DownloadStatus

}

/**
 * Represents the status of an upload
 */
sealed interface UploadStatus {

    /**
     * Represents the progress of an upload
     * @param totalBytesSend The total bytes sent
     * @param contentLength The total bytes to send
     */
    //TODO: Replace with multi-field value class
    data class Progress(val totalBytesSend: Long, val contentLength: Long) : UploadStatus

    /**
     * Represents the success of an upload
     * @param response The response of the upload
     */
    @JvmInline
    value class Success(val response: FileUploadResponse) : UploadStatus

}