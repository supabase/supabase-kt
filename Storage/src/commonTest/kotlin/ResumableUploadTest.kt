import app.cash.turbine.test
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ResumableUploadTest {

    private val bucketId = "bucketId"

    @Test
    fun testChunkSizeOfAFileLargerThanIntMaxValue() {
        runTest {
            val chunkSize = 1024L
            val size = Int.MAX_VALUE.toLong() + chunkSize
            val firstChunkLength = CompletableDeferred<Long>()
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Storage) {
                        resumable {
                            cache = MemoryResumableCache()
                            defaultChunkSize = chunkSize
                        }
                    }
                }
            ) { request ->
                when (request.method) {
                    HttpMethod.Post -> respond(
                        "",
                        HttpStatusCode.Created,
                        headersOf(HttpHeaders.Location, "https://projectref.supabase.co/upload")
                    )
                    HttpMethod.Patch -> {
                        firstChunkLength.complete(request.body.contentLength ?: -1)
                        respond(
                            "",
                            HttpStatusCode.NoContent,
                            headersOf("Upload-Offset", chunkSize.toString())
                        )
                    }
                    else -> respondError(HttpStatusCode.BadRequest)
                }
            }
            val upload = client.storage[bucketId].resumable.createOrContinueUpload(
                channel = { ByteReadChannel(ByteArray((chunkSize * 2).toInt())) },
                source = "source",
                size = size,
                path = "path"
            )
            upload.startOrResumeUploading()
            val length = withContext(Dispatchers.Default) {
                withTimeout(10.seconds) { firstChunkLength.await() }
            }
            upload.pause()
            assertEquals(chunkSize, length)
        }
    }

    @Test
    fun testEveryChunkIsSentWithTheUpsertOption() {
        runTest {
            val chunkSize = 1024L
            val size = chunkSize * 2
            val upsertHeaders = mutableListOf<String?>()
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Storage) {
                        resumable {
                            cache = MemoryResumableCache()
                            defaultChunkSize = chunkSize
                        }
                    }
                }
            ) { request ->
                when (request.method) {
                    HttpMethod.Post -> respond(
                        "",
                        HttpStatusCode.Created,
                        headersOf(HttpHeaders.Location, "https://projectref.supabase.co/upload")
                    )
                    HttpMethod.Patch -> {
                        upsertHeaders += request.headers["x-upsert"]
                        respond(
                            "",
                            HttpStatusCode.NoContent,
                            headersOf("Upload-Offset", (chunkSize * upsertHeaders.size).toString())
                        )
                    }
                    else -> respondError(HttpStatusCode.BadRequest)
                }
            }
            val upload = client.storage[bucketId].resumable.createOrContinueUpload(
                data = ByteArray(size.toInt()),
                source = "source",
                path = "path"
            ) {
                upsert = true
            }
            upload.startOrResumeUploading()
            upload.stateFlow.test {
                do {
                    val state = awaitItem()
                } while (state.status !is UploadStatus.Success)
            }
            assertEquals(listOf<String?>("true", "true"), upsertHeaders)
        }
    }

    @Test
    fun testAConflictingChunkIsResentFromTheServerOffset() {
        runTest {
            val patches = uploadTwoChunks(firstPatchStatus = HttpStatusCode.Conflict)
            assertEquals(listOf(0L to 1.toByte(), 0L to 1.toByte(), 1024L to 2.toByte()), patches)
        }
    }

    @Test
    fun testAFailedChunkIsResentFromTheServerOffset() {
        runTest {
            val patches = uploadTwoChunks(firstPatchStatus = HttpStatusCode.BadGateway)
            assertEquals(listOf(0L to 1.toByte(), 0L to 1.toByte(), 1024L to 2.toByte()), patches)
        }
    }

    private suspend fun uploadTwoChunks(firstPatchStatus: HttpStatusCode): List<Pair<Long, Byte>> {
        val chunkSize = 1024
        val patches = mutableListOf<Pair<Long, Byte>>()
        var serverOffset = 0L
        val client = createMockedSupabaseClient(
            configuration = {
                install(Storage) {
                    resumable {
                        cache = MemoryResumableCache()
                        defaultChunkSize = chunkSize.toLong()
                        retryTimeout = 10.milliseconds
                    }
                }
            }
        ) { request ->
            when (request.method) {
                HttpMethod.Post -> respond(
                    "",
                    HttpStatusCode.Created,
                    headersOf(HttpHeaders.Location, "https://projectref.supabase.co/upload")
                )
                HttpMethod.Head -> respond("", HttpStatusCode.OK, headersOf("Upload-Offset", serverOffset.toString()))
                HttpMethod.Patch -> {
                    val offset = request.headers["Upload-Offset"]!!.toLong()
                    val body = request.body.toByteArray()
                    patches += offset to body.first()
                    when {
                        patches.size == 1 -> respondError(firstPatchStatus)
                        offset != serverOffset -> respondError(HttpStatusCode.Conflict)
                        else -> {
                            serverOffset += body.size
                            respond("", HttpStatusCode.NoContent, headersOf("Upload-Offset", serverOffset.toString()))
                        }
                    }
                }
                else -> respondError(HttpStatusCode.BadRequest)
            }
        }
        val upload = client.storage[bucketId].resumable.createOrContinueUpload(
            data = ByteArray(chunkSize) { 1 } + ByteArray(chunkSize) { 2 },
            source = "source",
            path = "path"
        )
        upload.startOrResumeUploading()
        upload.stateFlow.test {
            do {
                val state = awaitItem()
            } while (state.status !is UploadStatus.Success)
        }
        return patches
    }

}
