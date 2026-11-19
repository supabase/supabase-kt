package io.supabase.storage.resumable

import io.supabase.storage.UploadStatus

/**
 * Represents the state of a resumable upload
 * @param fingerprint The fingerprint of the upload
 * @param status The current upload status
 * @param paused Whether the upload is paused
 */
data class ResumableUploadState(val fingerprint: Fingerprint, private val cacheEntry: ResumableCacheEntry, val status: UploadStatus, val paused: Boolean) {

    /**
     * The path for the upload
     */
    val path = cacheEntry.path

    /**
     * The bucket id for the upload
     */
    val bucketId = cacheEntry.bucketId

    /**
     * Whether the upload is done
     */
    val isDone = status is UploadStatus.Success

    /**
     * The upload progress as a float between 0 and 1
     */
    val progress = if(status is UploadStatus.Progress) {
         status.totalBytesSend.toFloat() / status.contentLength.toFloat()
    } else 1f

}