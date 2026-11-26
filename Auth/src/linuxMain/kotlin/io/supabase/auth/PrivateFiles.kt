package io.supabase.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.chmod
import platform.posix.getenv

/**
 * Reads and writes small files only the current user can read: the file is `0600`, a directory created for it
 * is `0700`, and a write replaces the file atomically.
 */
@OptIn(ExperimentalForeignApi::class)
internal object PrivateFiles {

    private val fileSystem = FileSystem.SYSTEM

    suspend fun read(file: Path): String? = withContext(Dispatchers.IO) {
        if (fileSystem.exists(file)) fileSystem.read(file) { readUtf8() } else null
    }

    suspend fun write(file: Path, content: String) = withContext(Dispatchers.IO) {
        // Only a directory created here is restricted: an existing one belongs to the app.
        file.parent?.takeUnless { fileSystem.exists(it) }?.let {
            fileSystem.createDirectories(it)
            chmod(it.toString(), DIRECTORY_MODE.convert())
        }
        val temporary = "$file.tmp".toPath()
        // Restrict the file before the content goes in.
        fileSystem.write(temporary) { }
        chmod(temporary.toString(), FILE_MODE.convert())
        fileSystem.write(temporary) { writeUtf8(content) }
        fileSystem.atomicMove(temporary, file)
    }

    suspend fun delete(file: Path) = withContext(Dispatchers.IO) {
        fileSystem.delete(file, mustExist = false)
    }

    /**
     * `$XDG_DATA_HOME/supabase-kt`, or `~/.local/share/supabase-kt` when `XDG_DATA_HOME` isn't an absolute path,
     * or null when neither is set.
     */
    fun defaultDirectory(): Path? {
        val dataHome = getenv("XDG_DATA_HOME")?.toKString()?.takeIf { it.startsWith("/") }
            ?: getenv("HOME")?.toKString()?.takeIf { it.isNotBlank() }?.let { "$it/.local/share" }
            ?: return null
        return dataHome.toPath() / DIRECTORY_NAME
    }

    private const val DIRECTORY_NAME = "supabase-kt"
    private const val DIRECTORY_MODE = 448 // 0700
    private const val FILE_MODE = 384 // 0600
}
