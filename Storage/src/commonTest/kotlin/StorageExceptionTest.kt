import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.storage.Storage
import io.supabase.storage.StorageRestException
import io.supabase.storage.storage
import io.supabase.testing.createMockedSupabaseClient
import io.supabase.testing.respondJson
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageExceptionTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Storage)
    }
    private lateinit var client: SupabaseClient

    @Test
    fun testUnauthorizedError() = runTest {
        mockStorageError(statusCode = HttpStatusCode.Unauthorized.value, error = "Unauthorized", message = "Invalid jwt")
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals("Unauthorized", exception.error)
        assertEquals("Invalid jwt", exception.description)
    }

    @Test
    fun testBadRequestError() = runTest {
        mockStorageError(statusCode = HttpStatusCode.BadRequest.value, error = "InvalidRequest", message = "Invalid bucket id")
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals("InvalidRequest", exception.error)
        assertEquals("Invalid bucket id", exception.description)
    }

    @Test
    fun testNotFoundError() = runTest {
        mockStorageError(statusCode = HttpStatusCode.NotFound.value, error = "NoSuchBucket", message = "Bucket not found")
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals("NoSuchBucket", exception.error)
        assertEquals("Bucket not found", exception.description)
    }

    @Test
    fun testOtherStatusCodesInBodyAreStorageRestException() = runTest {
        mockStorageError(statusCode = HttpStatusCode.Conflict.value, error = "Duplicate", message = "The resource already exists")
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals("Duplicate", exception.error)
        assertEquals("The resource already exists", exception.description)
    }

    @Test
    fun testNonBadRequestResponsesAreStorageRestException() = runTest {
        client = createMockedSupabaseClient(
            configuration = configureClient,
            requestHandler = {
                respondJson(
                    code = HttpStatusCode.NotFound,
                    data = buildJsonObject {
                        put("statusCode", HttpStatusCode.NotFound.value)
                        put("error", "NoSuchBucket")
                        put("message", "Bucket not found")
                        put("code", "NoSuchBucket")
                    }
                )
            }
        )
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals(HttpStatusCode.NotFound.value, exception.statusCode)
        assertEquals("NoSuchBucket", exception.error)
        assertEquals("Bucket not found", exception.description)
    }

    @Test
    fun testUnparsableErrorBody() = runTest {
        client = createMockedSupabaseClient(
            configuration = configureClient,
            requestHandler = {
                respond("maintenance", HttpStatusCode.BadRequest)
            }
        )
        val exception = assertFailsWith<StorageRestException> {
            client.storage.listBuckets()
        }
        assertEquals("Unknown error", exception.error)
        assertEquals(HttpStatusCode.BadRequest.value, exception.statusCode)
    }

    private fun mockStorageError(statusCode: Int, error: String, message: String) {
        client = createMockedSupabaseClient(
            configuration = configureClient,
            requestHandler = {
                respondJson(
                    code = HttpStatusCode.BadRequest,
                    data = buildJsonObject {
                        put("statusCode", statusCode)
                        put("error", error)
                        put("code", error)
                        put("message", message)
                    }
                )
            }
        )
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
