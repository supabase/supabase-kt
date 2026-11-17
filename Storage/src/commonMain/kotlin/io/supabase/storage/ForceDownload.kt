package io.supabase.storage

/**
 * Forces a download for a storage url with an optional [fileName]
 * @param fileName Set this parameter as the name of the file if you want to trigger the download with a different filename.
 */
data class ForceDownload(val fileName: String? = null)