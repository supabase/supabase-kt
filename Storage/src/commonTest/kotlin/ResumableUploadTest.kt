import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
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
            withContext(Dispatchers.Default) {
                withTimeout(10.seconds) { upload.stateFlow.first { it.status is UploadStatus.Success } }
            }
            assertEquals(listOf<String?>("true", "true"), upsertHeaders)
        }
    }

}
