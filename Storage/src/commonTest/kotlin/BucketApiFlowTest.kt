import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.storage.BucketApi
import io.supabase.storage.DownloadStatus
import io.supabase.storage.Storage
import io.supabase.storage.UploadData
import io.supabase.storage.UploadStatus
import io.supabase.storage.downloadAuthenticatedAsFlow
import io.supabase.storage.downloadPublicAsFlow
import io.supabase.storage.resumable.MemoryResumableCache
import io.supabase.storage.storage
import io.supabase.storage.updateAsFlow
import io.supabase.storage.uploadAsFlow
import io.supabase.storage.uploadToSignedUrlAsFlow
import io.supabase.testing.assertMethodIs
import io.supabase.testing.assertPathIs
import io.supabase.testing.pathAfterVersion
import io.supabase.testing.withMockedSupabaseClient
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.Flow
import kotlinx.io.readByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val dummyData = byteArrayOf(1, 2, 3)
private const val expectedToken = "123456"

class BucketApiFlowTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Storage) {
            resumable {
                cache = MemoryResumableCache()
            }
        }
    }
    private val bucketId = "bucketId"

    private fun testUploadAsFlow(
        upsert: Boolean,
        uploadAction: suspend (client: SupabaseClient, expectedPath: String, bytes: ByteArray) -> Unit
    ) {
        testUploadMethodWithByteArray(
            HttpMethod.Post,
            "/object/$bucketId/data.png",
            extra = {
                assertEquals(
                    upsert.toString(),
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be $upsert"
                )
            },
            test = uploadAction
        )
    }

    @Test
    fun testUploadAsFlowMethodWithByteArray() {
        testUploadAsFlow(upsert = false) { client, expectedPath, data ->
            val flow = client.storage[bucketId].uploadAsFlow(expectedPath, data)
            testUploadFlowWithByteArray(flow, expectedPath, data)
        }
    }

    @Test
    fun testUpsertAsFlowMethodWithByteArray() {
        testUploadAsFlow(upsert = true) { client, expectedPath, data ->
            val flow = client.storage[bucketId].uploadAsFlow(expectedPath, data) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, data)
        }
    }

    @Test
    fun testUploadAsFlowMethodWithChannel() {
        testUploadAsFlow(upsert = false) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].uploadAsFlow(expectedPath, data)
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    @Test
    fun testUpsertAsFlowMethodWithChannel() {
        testUploadAsFlow(upsert = true) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].uploadAsFlow(expectedPath, data) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    private fun testDownloadAsFlow(
        public: Boolean,
        downloadAction: suspend (client: SupabaseClient, path: String, expectedData: ByteArray) -> Unit
    ) {
        testDownloadMethod(
            urlPath = if (public) "/object/public/$bucketId/data.png" else "/object/authenticated/$bucketId/data.png",
            test = downloadAction
        )
    }

    @Test
    fun testDownloadAuthenticatedAsFlowMethodWithByteArray() {
        testDownloadAsFlow(public = false) { client, path, data ->
            val flow = client.storage[bucketId].downloadAuthenticatedAsFlow(path)
            testDownloadFlowWithByteArray(flow, data)
        }
    }

    @Test
    fun testDownloadPublicAsFlowMethodWithByteArray() {
        testDownloadAsFlow(public = true) { client, path, data ->
            val flow = client.storage[bucketId].downloadPublicAsFlow(path)
            testDownloadFlowWithByteArray(flow, data)
        }
    }

    @Test
    fun testDownloadAuthenticatedAsFlowMethodWithChannel() {
        testDownloadAsFlow(public = false) { client, path, data ->
            val channel = ByteChannel()
            val flow = client.storage[bucketId].downloadAuthenticatedAsFlow(path, channel)
            testDownloadFlowWithChannel(flow, channel, data)
        }
    }

    @Test
    fun testDownloadPublicAsFlowMethodWithChannel() {
        testDownloadAsFlow(public = true) { client, path, data ->
            val channel = ByteChannel()
            val flow = client.storage[bucketId].downloadPublicAsFlow(path, channel)
            testDownloadFlowWithChannel(flow, channel, data)
        }
    }

    private suspend fun testDownloadFlowWithByteArray(
        flow: Flow<DownloadStatus>,
        expectedData: ByteArray
    ) {
        flow.test {
            val (lastProgress, terminal) = awaitProgress()
            assertEquals(expectedData.size.toLong(), lastProgress.totalBytesReceived, "Total bytes received should be 3")
            assertEquals(expectedData.size.toLong(), lastProgress.contentLength, "Content length should be 3")
            assertIs<DownloadStatus.Success>(terminal)
            val dataStatus = awaitItem()
            assertIs<DownloadStatus.ByteData>(dataStatus)
            assertContentEquals(expectedData, dataStatus.data, "Data should be [1, 2, 3]")
            awaitComplete()
        }
    }

    private suspend fun testDownloadFlowWithChannel(
        flow: Flow<DownloadStatus>,
        channel: ByteChannel,
        expectedData: ByteArray
    ) {
        flow.test {
            //onDownload emits progress at engine-chosen points; under MockEngine the final
            //byte count isn't guaranteed before Success, so verify the channel content below instead
            val (_, terminal) = awaitProgress()
            assertIs<DownloadStatus.Success>(terminal) //When streaming to a channel, no ByteData is emitted
            awaitComplete()
        }
        assertContentEquals(expectedData, channel.readRemaining().readByteArray(), "Channel data should be [1, 2, 3]")
    }

    //Drains all progress statuses (a channel download may emit several) and returns the last
    //progress together with the first non-progress status
    private suspend fun ReceiveTurbine<DownloadStatus>.awaitProgress(): Pair<DownloadStatus.Progress, DownloadStatus> {
        var lastProgress: DownloadStatus.Progress? = null
        while (true) {
            val item = awaitItem()
            if (item is DownloadStatus.Progress) {
                lastProgress = item
            } else {
                assertIs<DownloadStatus.Progress>(lastProgress, "Expected at least one progress status before $item")
                return lastProgress to item
            }
        }
    }

    private fun testDownloadMethod(
        urlPath: String,
        expectedPath: String = "data.png",
        test: suspend (client: SupabaseClient, path: String, data: ByteArray) -> Unit
    ) {
        runTest {
            val expectedData = dummyData
            withMockedSupabaseClient(
                configuration = configureClient,
                requestHandler = {
                    assertMethodIs(HttpMethod.Get, it.method)
                    assertPathIs(urlPath, it.url.pathAfterVersion())
                    respond(
                        content = expectedData,
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf(ContentType.Image.PNG.toString()),
                            HttpHeaders.ContentLength to listOf(expectedData.size.toString())
                        )
                    )
                }
            ) { client ->
                test(client, expectedPath, expectedData)
            }
        }
    }

    private fun testUpdateAsFlow(
        upsert: Boolean,
        uploadAction: suspend (client: SupabaseClient, expectedPath: String, bytes: ByteArray) -> Unit
    ) {
        testUploadMethodWithByteArray(
            HttpMethod.Put,
            "/object/$bucketId/data.png",
            extra = {
                assertEquals(
                    upsert.toString(),
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be $upsert"
                )
            },
            test = uploadAction
        )
    }

    @Test
    fun testUpdateAsFlowMethodWithByteArray() {
        testUpdateAsFlow(upsert = false) { client, expectedPath, data ->
            val flow = client.storage[bucketId].updateAsFlow(expectedPath, data)
            testUploadFlowWithByteArray(flow, expectedPath, data)
        }
    }

    @Test
    fun testUpdateUpsertAsFlowMethodWithByteArray() {
        testUpdateAsFlow(upsert = true) { client, expectedPath, data ->
            val flow = client.storage[bucketId].updateAsFlow(expectedPath, data) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, data)
        }
    }

    @Test
    fun testUpdateAsFlowMethodWithChannel() {
        testUpdateAsFlow(upsert = false) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].updateAsFlow(expectedPath, data)
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    @Test
    fun testUpdateUpsertAsFlowMethodWithChannel() {
        testUpdateAsFlow(upsert = true) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].updateAsFlow(expectedPath, data) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    private fun testUploadToSignedUrl(
        upsert: Boolean,
        uploadAction: suspend (client: SupabaseClient, expectedPath: String, bytes: ByteArray) -> Unit
    ) {
        testUploadMethodWithByteArray(
            method = HttpMethod.Put,
            urlPath = "/object/upload/sign/$bucketId/data.png",
            extra = {
                assertEquals(
                    upsert.toString(),
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be $upsert"
                )
                assertEquals(
                    expectedToken,
                    it.url.parameters["token"],
                    "Token should be $expectedToken"
                )
            },
            test = uploadAction
        )
    }

    @Test
    fun testUploadToSignedUrlAsFlowMethodWithChannel() {
        testUploadToSignedUrl(upsert = false) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].uploadToSignedUrlAsFlow(expectedPath, expectedToken, data)
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    @Test
    fun testUploadToSignedUrlAsFlowMethodByteArray() {
        testUploadToSignedUrl(upsert = false) { client, expectedPath, bytes ->
            val flow = client.storage[bucketId].uploadToSignedUrlAsFlow(expectedPath, expectedToken, bytes)
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    @Test
    fun testUpsertToSignedUrlAsFlowMethodWithChannel() {
        testUploadToSignedUrl(upsert = true) { client, expectedPath, bytes ->
            val channel = ByteReadChannel(bytes)
            val expectedSize = bytes.size.toLong()
            val data = UploadData(channel, expectedSize)
            val flow = client.storage[bucketId].uploadToSignedUrlAsFlow(expectedPath, expectedToken, data) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    @Test
    fun testUpsertToSignedUrlAsFlowMethodByteArray() {
        testUploadToSignedUrl(upsert = true) { client, expectedPath, bytes ->
            val flow = client.storage[bucketId].uploadToSignedUrlAsFlow(expectedPath, expectedToken, bytes) {
                upsert = true
            }
            testUploadFlowWithByteArray(flow, expectedPath, bytes)
        }
    }

    private suspend fun testUploadFlowWithByteArray(
        flow: Flow<UploadStatus>,
        expectedPath: String,
        expectedData: ByteArray
    ) {
        flow.test {
            val progressStatus = awaitItem() //For the byte array there will be only one progress status
            assertIs<UploadStatus.Progress>(progressStatus)
            assertEquals(expectedData.size.toLong(), progressStatus.totalBytesSend, "Total bytes sent should be 3")
            assertEquals(expectedData.size.toLong(), progressStatus.contentLength, "Content length should be 3")
            val successStatus = awaitItem()
            assertIs<UploadStatus.Success>(successStatus)
            assertEquals(expectedPath, successStatus.response.path, "Path should be data.png")
            awaitComplete()
        }
    }

    private fun testUploadMethodWithByteArray(
        method: HttpMethod,
        urlPath: String,
        expectedPath: String = "data.png",
        extra: suspend MockRequestHandleScope.(HttpRequestData) -> Unit,
        test: suspend (client: SupabaseClient, expectedPath: String, data: ByteArray) -> Unit
    ) {
        runTest {
            val expectedData = dummyData
            withMockedSupabaseClient(
                configuration = configureClient,
                requestHandler = {
                    val data = it.body.toByteArray()
                    assertMethodIs(method, it.method)
                    assertPathIs(urlPath, it.url.pathAfterVersion())
                    assertContentEquals(expectedData, data, "Data should be [1, 2, 3]")
                    assertEquals(ContentType.Image.PNG, it.body.contentType, "Content type should be image/png")
                    extra(this, it)
                    respond(
                        content = """
                    { 
                        "Key": "$expectedPath",
                        "Id": "id"
                    }
                    """.trimIndent(),
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                    )
                }
            ) { client ->
                test(client, expectedPath, expectedData)
            }
        }
    }

}