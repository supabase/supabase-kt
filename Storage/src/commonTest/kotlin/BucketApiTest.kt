import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.FileObjectV2
import io.github.jan.supabase.storage.FileUploadResponse
import io.github.jan.supabase.storage.ImageTransformation
import io.github.jan.supabase.storage.SortOrder
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPath
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class BucketApiTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Storage) {
            resumable {
                cache = MemoryResumableCache()
            }
        }
    }
    private val bucketId = "bucketId"
    private lateinit var client: SupabaseClient

    @Test
    fun testCustomHeaders() {
        runTest {
            val expectedPrefixes = listOf("data.png", "data2.png", "data3.png")
            var index = 0
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs("/object/$bucketId", it.url.pathAfterVersion())
                if(index == 0) {
                    assertEquals("value", it.headers["customHeader"])
                    index++
                } else {
                    assertNull(it.headers["customHeader"])
                }
                respond("")
            }
            val otherBucketApi = client.storage[bucketId]
            client.storage[bucketId]
                .setHeader("customHeader", "value")
                .delete(expectedPrefixes)

            otherBucketApi.delete(expectedPrefixes)
        }
    }

    @Test
    fun testUpload() {
        testUploadMethod(
            method = HttpMethod.Post,
            urlPath = "/object/$bucketId/data.png",
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].upload(expectedPath, data) {
                    userMetadata = meta
                }
            },
            extra = {
                assertEquals(
                    "false",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be false"
                )
            }
        )
    }

    @Test
    fun testUploadUpsert() {
        testUploadMethod(
            method = HttpMethod.Post,
            urlPath = "/object/$bucketId/data.png",
            extra = {
                assertEquals(
                    "true",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be true"
                )
            },
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].upload(expectedPath, data) {
                    userMetadata = meta
                    upsert = true
                }
            }
        )
    }

    @Test
    fun testUploadToSignedUrl() {
        val expectedToken = "12345"
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/upload/sign/$bucketId/data.png",
            extra = {
                assertEquals(
                    "false",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be true"
                )
                assertEquals(
                    expectedToken,
                    it.url.parameters["token"],
                    "Token should be $expectedToken"
                )
            },
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].uploadToSignedUrl(path = expectedPath, token = expectedToken, data = data) {
                    userMetadata = meta
                    upsert = false
                }
            }
        )
    }

    @Test
    fun testUploadToSignedUrlWithoutIdInResponse() {
        runTest {
            client = createMockedSupabaseClient(configuration = configureClient) {
                respond(
                    content = """{ "Key": "$bucketId/data.png" }""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
            val response = client.storage[bucketId].uploadToSignedUrl(path = "data.png", token = "12345", data = byteArrayOf(1, 2, 3))
            assertEquals("$bucketId/data.png", response.key)
            assertNull(response.id)
            assertEquals("data.png", response.path)
        }
    }

    @Test
    fun testUploadToSignedUrlUpsert() {
        val expectedToken = "12345"
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/upload/sign/$bucketId/data.png",
            extra = {
                assertEquals(
                    "true",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be true"
                )
                assertEquals(
                    expectedToken,
                    it.url.parameters["token"],
                    "Token should be $expectedToken"
                )
            },
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].uploadToSignedUrl(path = expectedPath, token = expectedToken, data = data) {
                    userMetadata = meta
                    upsert = true
                }
            }
        )
    }

    @Test
    fun testUpdate() {
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/$bucketId/data.png",
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].update(expectedPath, data) {
                    userMetadata = meta
                }
            },
            extra = {
                assertEquals(
                    "false",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be false"
                )
            }
        )
    }

    @Test
    fun testUpdateUpsert() {
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/$bucketId/data.png",
            extra = {
                assertEquals(
                    "true",
                    it.headers[BucketApi.UPSERT_HEADER],
                    "Upsert header should be true"
                )
            },
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].update(expectedPath, data) {
                    userMetadata = meta
                    upsert = true
                }
            }
        )
    }

    @Test
    fun testUploadWithPathNeedingEncoding() {
        testUploadMethod(
            method = HttpMethod.Post,
            urlPath = "/object/$bucketId/folder/my%20f%C3%BCle.png",
            expectedPath = "folder/my füle.png",
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].upload(expectedPath, data) {
                    userMetadata = meta
                }
            },
            extra = {}
        )
    }

    @Test
    fun testUpdateWithPathNeedingEncoding() {
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/$bucketId/folder/my%20f%C3%BCle.png",
            expectedPath = "folder/my füle.png",
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].update(expectedPath, data) {
                    userMetadata = meta
                }
            },
            extra = {}
        )
    }

    @Test
    fun testUploadToSignedUrlWithPathNeedingEncoding() {
        testUploadMethod(
            method = HttpMethod.Put,
            urlPath = "/object/upload/sign/$bucketId/folder/my%20f%C3%BCle.png",
            expectedPath = "folder/my füle.png",
            request = { client, expectedPath, data, meta ->
                client.storage[bucketId].uploadToSignedUrl(path = expectedPath, token = "12345", data = data) {
                    userMetadata = meta
                }
            },
            extra = {}
        )
    }

    @Test
    fun testDelete() {
        runTest {
            val expectedPrefixes = listOf("data.png", "data2.png", "data3.png")
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs("/object/$bucketId", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject["prefixes"]?.jsonArray
                assertNotNull(body, "Prefixes element should not be null")
                assertContentEquals(expectedPrefixes, body.map { e -> e.jsonPrimitive.content }, "Body should be $expectedPrefixes")
                respond("")
            }
            client.storage[bucketId].delete(expectedPrefixes)
        }
    }

    @Test
    fun testPurgeCache() {
        runTest {
            val specialBucketId = "bucket name #1"
            val expectedPath = "data #1.png"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs(
                    "/cdn/${specialBucketId.encodeURLPath()}/${expectedPath.encodeURLPath()}",
                    it.url.pathAfterVersion()
                )
                assertEquals("true", it.url.parameters["transformations"], "Transformations should be true")
                respond("")
            }
            client.storage[specialBucketId].purgeCache(expectedPath)
        }
    }

    @Test
    fun testPurgeCacheWithoutTransformations() {
        runTest {
            val specialBucketId = "bucket name #1"
            val expectedPath = "data #1.png"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs(
                    "/cdn/${specialBucketId.encodeURLPath()}/${expectedPath.encodeURLPath()}",
                    it.url.pathAfterVersion()
                )
                assertNull(it.url.parameters["transformations"], "Transformations should not be set")
                respond("")
            }
            client.storage[specialBucketId].purgeCache(expectedPath) {
                transformations = false
            }
        }
    }

    @Test
    fun testMove() {
        runTest {
            val expectedFrom = "data.png"
            val expectedTo = "data2.png"
            val expectedToBucket = "bucket2"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/move", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                assertEquals(expectedFrom, content["sourceKey"]?.jsonPrimitive?.content, "From should be '$expectedFrom'")
                assertEquals(expectedTo, content["destinationKey"]?.jsonPrimitive?.content, "To should be '$expectedTo'")
                assertEquals(bucketId, content["bucketId"]?.jsonPrimitive?.content, "Bucket should be '$bucketId'")
                assertEquals(expectedToBucket, content["destinationBucket"]?.jsonPrimitive?.content, "To bucket should be '$expectedToBucket'")
                respond("")
            }
            client.storage[bucketId].move(expectedFrom, expectedTo, expectedToBucket)
        }
    }

    @Test
    fun testCopy() {
        runTest {
            val expectedFrom = "data.png"
            val expectedTo = "data2.png"
            val expectedToBucket = "bucket2"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/copy", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                assertEquals(expectedFrom, content["sourceKey"]?.jsonPrimitive?.content, "From should be '$expectedFrom'")
                assertEquals(expectedTo, content["destinationKey"]?.jsonPrimitive?.content, "To should be '$expectedTo'")
                assertEquals(bucketId, content["bucketId"]?.jsonPrimitive?.content, "Bucket should be '$bucketId'")
                assertEquals(expectedToBucket, content["destinationBucket"]?.jsonPrimitive?.content, "To bucket should be '$expectedToBucket'")
                respond("")
            }
            client.storage[bucketId].copy(expectedFrom, expectedTo, expectedToBucket)
        }
    }

    @Test
    fun testCreateSignedUrl() {
        runTest {
            val expectedPath = "folder/data.png"
            val expectedExpiresIn = 120.seconds
            val expectedHeight = 100
            val expectedWidth = 100
            val expectedQuality = 80
            val expectedResize = ImageTransformation.Resize.COVER
            val expectedFormat = "origin"
            val expectedUrl = "/object/sign/$bucketId/folder/data.png?token=12345"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/sign/$bucketId/$expectedPath", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                val transform = content["transform"]?.jsonObject
                assertEquals(
                    expectedExpiresIn,
                    content["expiresIn"]?.jsonPrimitive?.long?.seconds,
                    "Expires in should be $expectedExpiresIn"
                )
                assertEquals(expectedWidth, transform?.get("width")?.jsonPrimitive?.int, "Width should be $expectedWidth")
                assertEquals(expectedHeight, transform?.get("height")?.jsonPrimitive?.int, "Height should be $expectedHeight")
                assertEquals(expectedQuality, transform?.get("quality")?.jsonPrimitive?.int, "Quality should be $expectedQuality")
                assertEquals(expectedResize.name.lowercase(), transform?.get("resize")?.jsonPrimitive?.content, "Resize should be ${expectedResize.name.lowercase()}")
                assertEquals(expectedFormat, transform?.get("format")?.jsonPrimitive?.content, "Format should be $expectedFormat")
                assertNull(it.url.parameters["download"])
                assertNull(it.url.parameters["cacheNonce"])
                respond(
                    content = """
                    { 
                        "signedURL": "$expectedUrl"
                    }
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val url = client.storage[bucketId].createSignedUrl(expectedPath, expectedExpiresIn) {
                transform {
                    size(expectedWidth, expectedHeight)
                    format = expectedFormat
                    quality = expectedQuality
                    resize = expectedResize
                }
                forceDownload("myFile.png")
                cacheNonce = "1234"
            }
            assertEquals(client.storage.resolveUrl(expectedUrl.substring(1)) + "&cacheNonce=1234&download=myFile.png", url)
        }
    }

    @Test
    fun testCreateSignedUrlEmptyTransform() {
        runTest {
            val expectedPath = "folder/data.png"
            val expectedExpiresIn = 120.seconds
            val expectedUrl = "/object/sign/$bucketId/folder/data.png?token=12345"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/sign/$bucketId/$expectedPath", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                assertNull(content["transform"]?.jsonObject)
                assertEquals(
                    expectedExpiresIn,
                    content["expiresIn"]?.jsonPrimitive?.long?.seconds,
                    "Expires in should be $expectedExpiresIn"
                )
                respond(
                    content = """
                    { 
                        "signedURL": "$expectedUrl"
                    }
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val url = client.storage[bucketId].createSignedUrl(expectedPath, expectedExpiresIn)
            assertEquals(client.storage.resolveUrl(expectedUrl.substring(1)), url, "URL should be $expectedUrl")
        }
    }

    @Test
    fun testCreateSignedUrls() {
        runTest {
            val expectedPaths = listOf("folder/data.png", "folder/data2.png", "folder/data3.png")
            val expectedExpiresIn = 120.seconds
            val expectedUrls = listOf(
                "/object/sign/$bucketId/folder/data.png?token=11111",
                "/object/sign/$bucketId/folder/data2.png?token=22222",
                "/object/sign/$bucketId/folder/data3.png?token=33333"
            )
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/sign/$bucketId", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                val paths = content["paths"]?.jsonArray
                assertNotNull(paths, "Paths element should not be null")
                assertContentEquals(expectedPaths, paths.map { e -> e.jsonPrimitive.content }, "Paths should be $expectedPaths")
                assertEquals(
                    expectedExpiresIn,
                    content["expiresIn"]?.jsonPrimitive?.long?.seconds,
                    "Expires in should be $expectedExpiresIn"
                )
                assertNull(it.url.parameters["cacheNonce"])
                assertNull(it.url.parameters["download"])
                respond(
                    content = """
                    [
                        { "signedURL": "${expectedUrls[0]}", "path": "${expectedPaths[0]}" },
                        { "signedURL": "${expectedUrls[1]}", "path": "${expectedPaths[1]}" },
                        { "signedURL": "${expectedUrls[2]}", "path": "${expectedPaths[2]}" }
                    ]
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val urls = client.storage[bucketId].createSignedUrls(expectedExpiresIn, expectedPaths) {
                forceDownload("myFile.png")
                cacheNonce = "1234"
            }
            assertEquals(expectedUrls.map { client.storage.resolveUrl(it.substring(1)) + "&cacheNonce=1234&download=myFile.png" }, urls.map { it.signedURL })
        }
    }

    @Test
    fun testCreateSignedUploadUrl() {
        runTest {
            val expectedPath = "folder/data.png"
            val expectedToken = "12345"
            val expectedUrl =
                "/object/upload/sign/$bucketId/folder/data.png?token=$expectedToken"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs(
                    "/object/upload/sign/$bucketId/$expectedPath",
                    it.url.pathAfterVersion()
                )
                respond(
                    content = """
                    { 
                        "url": "$expectedUrl"
                    }
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val url = client.storage[bucketId].createSignedUploadUrl(expectedPath)
            assertEquals(expectedToken, url.token, "Token should be $expectedToken")
            assertEquals(client.storage.resolveUrl(expectedUrl.substring(1)), url.url, "URL should be $expectedUrl")
            assertEquals(expectedPath, url.path, "Path should be $expectedPath")
        }
    }

    @Test
    fun testDownloadAuthenticated() {
        runTest {
            val expectedPath = "data.png"
            val expectedData = byteArrayOf(1, 2, 3)
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/object/authenticated/$bucketId/$expectedPath", it.url.pathAfterVersion())
                assertEquals("1234", it.url.parameters["cacheNonce"])
                respond(expectedData)
            }
            val data = client.storage[bucketId].downloadAuthenticated(expectedPath) { cacheNonce = "1234" }
            assertContentEquals(expectedData, data, "Data should be [1, 2, 3]")
        }
    }

    @Test
    fun testDownloadPublic() {
        runTest {
            val expectedPath = "data.png"
            val expectedData = byteArrayOf(1, 2, 3)
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/object/public/$bucketId/$expectedPath", it.url.pathAfterVersion())
                respond(expectedData)
            }
            val data = client.storage[bucketId].downloadPublic(expectedPath)
            assertContentEquals(expectedData, data, "Data should be [1, 2, 3]")
        }
    }

    @Test
    fun testDownloadThrowsExceptionForBothVariants() = runTest {
        val supabase = createMockedSupabaseClient(configuration = {
            install(Storage)
        }) {
            respond(
                content = """{"statusCode":404,"error":"not_found","message":"Object not found"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val bucket = supabase.storage.from("photos")

        // The ByteArray overload throws a RestException
        val bytesFailure = runCatching { bucket.downloadAuthenticated("missing.jpg") {} }.exceptionOrNull()
        assertIs<RestException>(bytesFailure)

        // The channel overload should throw too, and write nothing
        val channel = ByteChannel(autoFlush = true)
        val channelFailure = runCatching { bucket.downloadAuthenticated("missing.jpg", channel) {} }.exceptionOrNull()
        runCatching { channel.flushAndClose() } // a failing download may leave the channel open
        val written = channel.readRemaining().readByteArray()

        assertNotNull(channelFailure)
        assertEquals(0, written.size)
    }

    @Test
    fun testDownloadAuthenticatedWithTransform() {
        testDownloadWithTransform(authenticated = true)
    }

    @Test
    fun testDownloadPublicWithTransform() {
        testDownloadWithTransform(authenticated = false)
    }

    @Test
    fun testList() {
        runTest {
            val expectedLimit = 10
            val expectedOffset = 5
            val expectedSearch = "vectors/data"
            val expectedColumn = "name"
            val expectedOrder = "asc"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/object/list/$bucketId", it.url.pathAfterVersion())
                val content = it.body.toJsonElement().jsonObject
                val sortBy = content["sortBy"]?.jsonObject
                assertEquals(expectedLimit, content["limit"]?.jsonPrimitive?.int, "Limit should be $expectedLimit")
                assertEquals(expectedOffset, content["offset"]?.jsonPrimitive?.int, "Offset should be $expectedOffset")
                assertEquals(expectedSearch, content["search"]?.jsonPrimitive?.content, "Search should be $expectedSearch")
                assertEquals(expectedColumn, sortBy?.get("column")?.jsonPrimitive?.content, "Column should be $expectedColumn")
                assertEquals(expectedOrder, sortBy?.get("order")?.jsonPrimitive?.content, "Order should be $expectedOrder")
                respond(
                    content = """
                    [
                      {
                        "name": "string",
                        "bucket_id": "string",
                        "id": "string",
                        "updated_at": "${Clock.System.now()}",
                        "created_at": "${Clock.System.now()}",
                        "last_accessed_at": "${Clock.System.now()}",
                        "metadata": {}
                      }
                    ]
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val data = client.storage[bucketId].list {
                limit = expectedLimit
                offset = expectedOffset
                search = expectedSearch
                sortBy(expectedColumn, SortOrder.ASC)
            }
         //   assertContentEquals(expectedData, data, "Data should be $expectedData")
        }
    }

    @Test
    fun testInfo() {
        runTest {
            val expectedPath = "data.png"
            val file = FileObjectV2(
                "data.png",
                "id",
                "version",
                createdAt = Clock.System.now(),
                metadata = null,
                size = 0,
                rawContentType = "image/png",
                etag = null,
                lastModified = null,
                cacheControl = null
            )
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/object/info/$bucketId/$expectedPath", it.url.pathAfterVersion())
                respond(
                    content = Json.encodeToString(file),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val data = client.storage[bucketId].info(expectedPath)
            assertEquals(file.copy(serializer = client.storage.serializer), data, "Data should be $file")
        }
    }

    @Test
    fun testExistsWithExistingFile() {
        runTest {
            val expectedPath = "data.png"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Head, it.method)
                assertPathIs("/object/$bucketId/$expectedPath", it.url.pathAfterVersion())
                respondOk()
            }
            val exists = client.storage[bucketId].exists(expectedPath)
            assertTrue { exists }
        }
    }

    @Test
    fun testExistsWithNonExistingFile() {
        val statusCodes = listOf(404, 400)
        for(code in statusCodes) {
            runTest {
                val expectedPath = "data.png"
                client = createMockedSupabaseClient(configuration = configureClient) {
                    assertMethodIs(HttpMethod.Head, it.method)
                    assertPathIs("/object/$bucketId/$expectedPath", it.url.pathAfterVersion())
                    respondError(HttpStatusCode(code, "Not Found"))
                }
                val exists = client.storage[bucketId].exists(expectedPath)
                assertFalse { exists }
            }
        }
    }

    private fun testDownloadWithTransform(
        authenticated: Boolean
    ) {
        runTest {
            val expectedPath = "data.png"
            val expectedData = byteArrayOf(1, 2, 3)
            val expectedHeight = 100
            val expectedWidth = 100
            val expectedQuality = 80
            val expectedResize = ImageTransformation.Resize.COVER
            val expectedFormat = "origin"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertMethodIs(HttpMethod.Get, it.method)
                val path = if(authenticated) "authenticated" else "public"
                assertPathIs("/render/image/$path/$bucketId/$expectedPath", it.url.pathAfterVersion())
                val content = it.url.parameters
                assertEquals(expectedWidth.toString(), content["width"], "Width should be $expectedWidth")
                assertEquals(expectedHeight.toString(), content["height"], "Height should be $expectedHeight")
                assertEquals(expectedQuality.toString(), content["quality"], "Quality should be $expectedQuality")
                assertEquals(expectedResize.name.lowercase(), content["resize"], "Resize should be ${expectedResize.name.lowercase()}")
                assertEquals(expectedFormat, content["format"], "Format should be $expectedFormat")
                assertEquals("1234", it.url.parameters["cacheNonce"])
                respond(expectedData)
            }
            val transform: ImageTransformation.() -> Unit = {
                size(expectedWidth, expectedHeight)
                format = expectedFormat
                quality = expectedQuality
                resize = expectedResize
            }
            val data = if(authenticated) {
                client.storage[bucketId].downloadAuthenticated(expectedPath) { transform(transform); cacheNonce = "1234" }
            } else {
                client.storage[bucketId].downloadPublic(expectedPath) { transform(transform); cacheNonce = "1234" }
            }
            assertContentEquals(expectedData, data, "Data should be [1, 2, 3]")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun testUploadMethod(
        method: HttpMethod,
        urlPath: String,
        expectedPath: String = "data.png",
        extra: suspend MockRequestHandleScope.(HttpRequestData) -> Unit,
        request: suspend (client: SupabaseClient, expectedPath: String, data: ByteArray, metadata: JsonObject) -> FileUploadResponse
    ) {
        runTest {
            val expectedData = byteArrayOf(1, 2, 3)
            val expectedMetadata = buildJsonObject {
                put("key", "value")
            }
            client = createMockedSupabaseClient(configuration = configureClient) {
                val data = it.body.toByteArray()
                assertMethodIs(method, it.method)
                val metadata = Json.decodeFromString<JsonObject>(Base64.decode(it.headers["x-metadata"] ?: error("Metadata should not be null")).decodeToString())
                assertEquals(expectedMetadata, metadata, "Metadata should be $expectedMetadata")
                assertPathIs(urlPath, it.url.pathAfterVersion())
                assertContentEquals(expectedData, data, "Data should be [1, 2, 3]")
                assertEquals(ContentType.Image.PNG, it.body.contentType, "Content type should be image/png")
                extra(this, it)
                respond(
                    content = """
                    { 
                        "Key": "someBucket/$expectedPath",
                        "Id": "someId"
                    }
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
            val response = request(client, expectedPath, expectedData, expectedMetadata)
            assertEquals("someBucket/$expectedPath", response.key, "Key should be $expectedPath")
            assertEquals("someId", response.id, "Id should be someId")
            assertEquals(expectedPath, response.path, "Path should be $expectedPath")
        }
    }

    @AfterTest
    fun cleanup() {
        runTest {
            if(::client.isInitialized) {
                client.close()
            }
        }
    }

}