import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.storage.BucketSortColumn
import io.github.jan.supabase.storage.SortOrder
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.github.jan.supabase.storage.resumable.ResumableClientImpl
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Clock

class StorageTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Storage) {
            resumable {
                cache = MemoryResumableCache()
            }
        }
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testCreateBucket() {
        runTest {
            val name = "test-bucket"
            val public = true
            val fileSizeLimit = "20mb"
            val allowedMimeType = listOf("image/jpeg", "image/png")
            client = createMockedSupabaseClient(configuration = configureClient) {
                val body = it.body.toJsonElement().jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/bucket", it.url.pathAfterVersion())
                assertEquals(body["name"]?.jsonPrimitive?.content, name, "Name should be $name")
                assertEquals(body["public"]?.jsonPrimitive?.boolean, public, "Public should be $public")
                assertEquals(body["file_size_limit"]?.jsonPrimitive?.content, fileSizeLimit, "File size limit should be $fileSizeLimit")
                assertEquals(body["allowed_mime_types"]?.jsonArray?.toStringArray(), allowedMimeType, "Allowed mime type should be $allowedMimeType")
                respond("")
            }
            client.storage.createBucket(name) {
                this.public = public
                this.fileSizeLimit = 20.megabytes
                allowedMimeTypes(*allowedMimeType.toTypedArray())
            }
        }
    }

    @Test
    fun testUpdateBucket() {
        runTest {
            val name = "test-bucket"
            val public = false
            val fileSizeLimit = "10mb"
            val allowedMimeType = listOf("image/jpeg")
            client = createMockedSupabaseClient(configuration = configureClient) {
                val body = it.body.toJsonElement().jsonObject
                assertPathIs("/bucket/$name", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Put, it.method)
                assertEquals(body["public"]?.jsonPrimitive?.boolean, public, "Public should be $public")
                assertEquals(body["file_size_limit"]?.jsonPrimitive?.content, fileSizeLimit, "File size limit should be $fileSizeLimit")
                assertEquals(body["allowed_mime_types"]?.jsonArray?.toStringArray(), allowedMimeType, "Allowed mime type should be $allowedMimeType")
                respond("")
            }
            client.storage.updateBucket(name) {
                this.public = public
                this.fileSizeLimit = 10.megabytes
                allowedMimeTypes(*allowedMimeType.toTypedArray())
            }
        }
    }

    @Test
    fun testDeleteBucket() {
        runTest {
            val name = "test-bucket"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/bucket/$name", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respond("")
            }
            client.storage.deleteBucket(name)
        }
    }

    @Test
    fun testEmptyBucket() {
        runTest {
            val name = "test-bucket"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/bucket/$name/empty", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                respond("")
            }
            client.storage.emptyBucket(name)
        }
    }

    @Test
    fun testPurgeBucketCache() {
        runTest {
            val name = "bucket name #1"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/cdn/${name.encodeURLPath()}", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                assertEquals("true", it.url.parameters["transformations"], "Transformations should be true")
                respond("")
            }
            client.storage.purgeBucketCache(name)
        }
    }

    @Test
    fun testPurgeBucketCacheWithoutTransformations() {
        runTest {
            val name = "bucket name #1"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/cdn/${name.encodeURLPath()}", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                assertNull(it.url.parameters["transformations"], "Transformations should not be set")
                respond("")
            }
            client.storage.purgeBucketCache(name) {
                transformations = false
            }
        }
    }

    @Test
    fun testListBuckets() {
        runTest {
            val expectedId = "test-bucket"
            val expectedPublic = true
            val expectedFileSizeLimit = 10000L
            val expectedAllowedMimeTypes = listOf("image/jpeg", "image/png")
            val expectedCreatedAt = Clock.System.now()
            val expectedUpdatedAt = Clock.System.now()
            val owner = "uuid"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/bucket", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respond(
                    """
                    [
                        ${createSampleBucket(
                            id = expectedId,
                            name = expectedId,
                            public = expectedPublic,
                            fileSizeLimit = expectedFileSizeLimit,
                            allowedMimeTypes = expectedAllowedMimeTypes,
                            createdAt = expectedCreatedAt.toString(),
                            updatedAt = expectedUpdatedAt.toString(),
                            owner = owner
                        )}
                    ]
                    """
                )
            }
            val buckets = client.storage.listBuckets()
            assertEquals(1, buckets.size, "Buckets should contain 1 item")
            assertEquals(expectedId, buckets[0].id, "Bucket id should be 'test-bucket'")
            assertEquals(expectedId, buckets[0].name, "Bucket name should be 'test-bucket'")
            assertEquals(expectedPublic, buckets[0].public, "Bucket public should be true")
            assertEquals(expectedFileSizeLimit, buckets[0].fileSizeLimit, "Bucket file size limit should be 10000")
            assertEquals(expectedCreatedAt, buckets[0].createdAt, "Bucket created at should be $expectedCreatedAt")
            assertEquals(expectedUpdatedAt, buckets[0].updatedAt, "Bucket updated at should be $expectedUpdatedAt")
            assertEquals(owner, buckets[0].owner, "Bucket owner should be $owner")
            assertEquals(expectedAllowedMimeTypes, buckets[0].allowedMimeTypes, "Bucket allowed mime types should be ['image/jpeg', 'image/png']")
        }
    }

    @Test
    fun testListBucketsWithFilter() {
        runTest {
            val expectedId = "test-bucket"
            val expectedPublic = true
            val expectedFileSizeLimit = 10000L
            val expectedAllowedMimeTypes = listOf("image/jpeg", "image/png")
            val expectedCreatedAt = Clock.System.now()
            val expectedUpdatedAt = Clock.System.now()
            val owner = "uuid"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/bucket", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals("10", it.url.parameters["limit"], "Limit should be 10")
                assertEquals("5", it.url.parameters["offset"], "Offset should be 5")
                assertEquals("test", it.url.parameters["search"], "Search should be 'test'")
                assertEquals("asc", it.url.parameters["sortOrder"], "Sort order should be 'asc'")
                assertEquals("name", it.url.parameters["sortColumn"], "Sort column should be 'name'")
                respond(
                    """
                    [
                        ${
                        createSampleBucket(
                            id = expectedId,
                            name = expectedId,
                            public = expectedPublic,
                            fileSizeLimit = expectedFileSizeLimit,
                            allowedMimeTypes = expectedAllowedMimeTypes,
                            createdAt = expectedCreatedAt.toString(),
                            updatedAt = expectedUpdatedAt.toString(),
                            owner = owner
                        )
                    }
                    ]
                    """
                )
            }
            val buckets = client.storage.listBuckets {
                limit = 10
                offset = 5
                search = "test"
                sortBy(BucketSortColumn.NAME, SortOrder.ASC)
            }
            assertEquals(1, buckets.size, "Buckets should contain 1 item")
            assertEquals(expectedId, buckets[0].id, "Bucket id should be 'test-bucket'")
        }
    }

    @Test
    fun testRetrieveBucket() {
        runTest {
            val expectedId = "test-bucket"
            val expectedPublic = true
            val expectedFileSizeLimit = 10000L
            val expectedAllowedMimeTypes = listOf("image/jpeg", "image/png")
            val expectedCreatedAt = Clock.System.now()
            val expectedUpdatedAt = Clock.System.now()
            val owner = "uuid"
            client = createMockedSupabaseClient(configuration = configureClient) {
                assertPathIs("/bucket/$expectedId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respond(
                    """
                    ${createSampleBucket(
                        id = expectedId,
                        name = expectedId,
                        public = expectedPublic,
                        fileSizeLimit = expectedFileSizeLimit,
                        allowedMimeTypes = expectedAllowedMimeTypes,
                        createdAt = expectedCreatedAt.toString(),
                        updatedAt = expectedUpdatedAt.toString(),
                        owner = owner
                    )}
                    """
                )
            }
            val bucket = client.storage.getBucket(expectedId)
            assertEquals(expectedId, bucket?.id, "Bucket id should be 'test-bucket'")
            assertEquals(expectedId, bucket?.name, "Bucket name should be 'test-bucket'")
            assertEquals(expectedPublic, bucket?.public, "Bucket public should be true")
            assertEquals(expectedFileSizeLimit, bucket?.fileSizeLimit, "Bucket file size limit should be 10000")
            assertEquals(expectedCreatedAt, bucket?.createdAt, "Bucket created at should be $expectedCreatedAt")
            assertEquals(expectedUpdatedAt, bucket?.updatedAt, "Bucket updated at should be $expectedUpdatedAt")
            assertEquals(owner, bucket?.owner, "Bucket owner should be $owner")
            assertEquals(expectedAllowedMimeTypes, bucket?.allowedMimeTypes, "Bucket allowed mime types should be ['image/jpeg', 'image/png']")
        }
    }

    @Test
    fun testAuthHeaderWhenAuthInstalled() {
        runTest {
            val key = "test-key"
            client = createMockedSupabaseClient(
                configuration = {
                    install(Storage) {
                        resumable {
                            cache = MemoryResumableCache()
                        }
                    }
                    install(Auth) {
                        minimalConfig()
                        enableLifecycleCallbacks = false
                    }
                }
            ) {
                assertEquals("Bearer $key", it.headers["Authorization"], "Authorization header should be 'Bearer test-key'")
                respond("[]")
            }
            client.auth.awaitInitialization()
            client.auth.importAuthToken(key)
            client.storage.listBuckets()
        }
    }

    @Test
    fun testBucketsShareTheDefaultResumableCache() {
        runTest {
            client = createMockedSupabaseClient(configuration = { install(Storage) }) {
                respond("")
            }
            val first = client.storage.from("first").resumable as ResumableClientImpl
            val second = client.storage.from("second").resumable as ResumableClientImpl
            assertSame(first.cache, second.cache)
            assertSame(first.cache, (client.storage.from("first").resumable as ResumableClientImpl).cache)
        }
    }

    private fun createSampleBucket(
        id: String,
        name: String,
        public: Boolean,
        fileSizeLimit: Long,
        allowedMimeTypes: List<String>,
        createdAt: String,
        updatedAt: String,
        owner: String
    ): String {
        return """
            {
                "id": "$id",
                "name": "$name",
                "public": $public,
                "file_size_limit": $fileSizeLimit,
                "allowed_mime_types": ${allowedMimeTypes.joinToString(prefix = "[", postfix = "]", transform = { s -> "\"$s\"" })},
                "created_at": "$createdAt",
                "updated_at": "$updatedAt",
                "owner": "$owner"
            }
        """
    }


    private fun JsonArray.toStringArray(): List<String> {
        return map { it.jsonPrimitive.content }
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