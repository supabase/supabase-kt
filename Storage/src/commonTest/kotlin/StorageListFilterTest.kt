import io.supabase.storage.BucketSortColumn
import io.supabase.storage.SortOrder
import io.supabase.storage.StorageListFilter
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the unified StorageListFilter class.
 *
 * StorageListFilter.Buckets is used for listing buckets via Storage.listBuckets()
 * StorageListFilter.Files is used for listing files within a bucket via BucketApi.list()
 */
class StorageListFilterTest {

    // ==================== StorageListFilter.Buckets Tests ====================

    @Test
    fun testBucketsFilterWithAllParameters() {
        val filter = StorageListFilter.Buckets().apply {
            limit = 10
            offset = 5
            search = "test"
            sortBy(BucketSortColumn.NAME, SortOrder.ASC)
        }
        val params = filter.buildParameters()
        assertEquals("10", params["limit"])
        assertEquals("5", params["offset"])
        assertEquals("test", params["search"])
        assertEquals("asc", params["sortOrder"])
        assertEquals("name", params["sortColumn"])
    }

    @Test
    fun testBucketsFilterEmpty() {
        val filter = StorageListFilter.Buckets()
        val params = filter.buildParameters()
        assertNull(params["limit"])
        assertNull(params["offset"])
        assertNull(params["search"])
        assertNull(params["sortOrder"])
        assertNull(params["sortColumn"])
    }

    @Test
    fun testBucketsFilterIndividualParameters() {
        // Test limit only
        var filter = StorageListFilter.Buckets().apply { limit = 20 }
        var params = filter.buildParameters()
        assertEquals("20", params["limit"])
        assertNull(params["offset"])

        // Test offset only
        filter = StorageListFilter.Buckets().apply { offset = 15 }
        params = filter.buildParameters()
        assertEquals("15", params["offset"])
        assertNull(params["limit"])

        // Test search only
        filter = StorageListFilter.Buckets().apply { search = "my-bucket" }
        params = filter.buildParameters()
        assertEquals("my-bucket", params["search"])
        assertNull(params["limit"])
    }

    @Test
    fun testBucketsFilterSortColumns() {
        // Test all sort columns with both orders
        val columns = listOf(
            BucketSortColumn.ID to "id",
            BucketSortColumn.NAME to "name",
            BucketSortColumn.CREATED_AT to "created_at",
            BucketSortColumn.UPDATED_AT to "updated_at"
        )

        for ((column, expectedName) in columns) {
            // Test ascending
            var filter = StorageListFilter.Buckets().apply { sortBy(column, SortOrder.ASC) }
            var params = filter.buildParameters()
            assertEquals(expectedName, params["sortColumn"])
            assertEquals("asc", params["sortOrder"])

            // Test descending
            filter = StorageListFilter.Buckets().apply { sortBy(column, SortOrder.DESC) }
            params = filter.buildParameters()
            assertEquals(expectedName, params["sortColumn"])
            assertEquals("desc", params["sortOrder"])
        }
    }

    @Test
    fun testBucketsFilterEdgeCases() {
        // Zero values
        var filter = StorageListFilter.Buckets().apply {
            limit = 0
            offset = 0
        }
        var params = filter.buildParameters()
        assertEquals("0", params["limit"])
        assertEquals("0", params["offset"])

        // Empty search string
        filter = StorageListFilter.Buckets().apply { search = "" }
        params = filter.buildParameters()
        assertEquals("", params["search"])

        // Special characters in search
        filter = StorageListFilter.Buckets().apply { search = "test-bucket_123" }
        params = filter.buildParameters()
        assertEquals("test-bucket_123", params["search"])

        // Large numbers
        filter = StorageListFilter.Buckets().apply {
            limit = 1000
            offset = 5000
        }
        params = filter.buildParameters()
        assertEquals("1000", params["limit"])
        assertEquals("5000", params["offset"])
    }

    @Test
    fun testBucketsFilterCombinations() {
        // Limit and offset
        var filter = StorageListFilter.Buckets().apply {
            limit = 25
            offset = 50
        }
        var params = filter.buildParameters()
        assertEquals("25", params["limit"])
        assertEquals("50", params["offset"])
        assertNull(params["search"])

        // Search and sort
        filter = StorageListFilter.Buckets().apply {
            search = "images"
            sortBy(BucketSortColumn.UPDATED_AT, SortOrder.ASC)
        }
        params = filter.buildParameters()
        assertEquals("images", params["search"])
        assertEquals("updated_at", params["sortColumn"])
        assertEquals("asc", params["sortOrder"])

        // Pagination with sort
        filter = StorageListFilter.Buckets().apply {
            limit = 10
            offset = 30
            sortBy(BucketSortColumn.NAME, SortOrder.ASC)
        }
        params = filter.buildParameters()
        assertEquals("10", params["limit"])
        assertEquals("30", params["offset"])
        assertEquals("name", params["sortColumn"])
        assertEquals("asc", params["sortOrder"])
    }

    // ==================== StorageListFilter.Files Tests ====================

    @Test
    fun testFilesFilterWithAllParameters() {
        val filter = StorageListFilter.Files().apply {
            limit = 10
            offset = 0
            search = "string"
            sortBy("name", SortOrder.ASC)
        }
        val filterJson = filter.buildBody()
        assertEquals(10, filterJson["limit"]!!.jsonPrimitive.int)
        assertEquals(0, filterJson["offset"]!!.jsonPrimitive.int)
        assertEquals("string", filterJson["search"]!!.jsonPrimitive.content)
        assertEquals("name", filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
        assertEquals("asc", filterJson["sortBy"]!!.jsonObject["order"]!!.jsonPrimitive.content)
    }

    @Test
    fun testFilesFilterEmpty() {
        val filter = StorageListFilter.Files()
        val filterJson = filter.buildBody()
        assertNull(filterJson["limit"])
        assertNull(filterJson["offset"])
        assertNull(filterJson["search"])
        assertNull(filterJson["sortBy"])
    }

    @Test
    fun testFilesFilterIndividualParameters() {
        // Test limit only
        var filter = StorageListFilter.Files().apply { limit = 25 }
        var filterJson = filter.buildBody()
        assertEquals(25, filterJson["limit"]!!.jsonPrimitive.int)
        assertNull(filterJson["offset"])

        // Test offset only
        filter = StorageListFilter.Files().apply { offset = 50 }
        filterJson = filter.buildBody()
        assertEquals(50, filterJson["offset"]!!.jsonPrimitive.int)
        assertNull(filterJson["limit"])

        // Test search only
        filter = StorageListFilter.Files().apply { search = "my-file" }
        filterJson = filter.buildBody()
        assertEquals("my-file", filterJson["search"]!!.jsonPrimitive.content)
        assertNull(filterJson["limit"])

        // Test sort only
        filter = StorageListFilter.Files().apply { sortBy("created_at", SortOrder.DESC) }
        filterJson = filter.buildBody()
        assertEquals("created_at", filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
        assertEquals("desc", filterJson["sortBy"]!!.jsonObject["order"]!!.jsonPrimitive.content)
        assertNull(filterJson["limit"])
    }

    @Test
    fun testFilesFilterSortColumns() {
        // Test common sort columns with both orders
        val columns = listOf("id", "name", "created_at", "updated_at")

        for (column in columns) {
            // Test ascending
            var filter = StorageListFilter.Files().apply { sortBy(column, SortOrder.ASC) }
            var filterJson = filter.buildBody()
            assertEquals(column, filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
            assertEquals("asc", filterJson["sortBy"]!!.jsonObject["order"]!!.jsonPrimitive.content)

            // Test descending
            filter = StorageListFilter.Files().apply { sortBy(column, SortOrder.DESC) }
            filterJson = filter.buildBody()
            assertEquals(column, filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
            assertEquals("desc", filterJson["sortBy"]!!.jsonObject["order"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun testFilesFilterEdgeCases() {
        // Zero values
        var filter = StorageListFilter.Files().apply {
            limit = 0
            offset = 0
        }
        var filterJson = filter.buildBody()
        assertEquals(0, filterJson["limit"]!!.jsonPrimitive.int)
        assertEquals(0, filterJson["offset"]!!.jsonPrimitive.int)

        // Empty search string
        filter = StorageListFilter.Files().apply { search = "" }
        filterJson = filter.buildBody()
        assertEquals("", filterJson["search"]!!.jsonPrimitive.content)

        // Special characters in search
        filter = StorageListFilter.Files().apply { search = "file-name_123.png" }
        filterJson = filter.buildBody()
        assertEquals("file-name_123.png", filterJson["search"]!!.jsonPrimitive.content)

        // Large numbers
        filter = StorageListFilter.Files().apply {
            limit = 1000
            offset = 5000
        }
        filterJson = filter.buildBody()
        assertEquals(1000, filterJson["limit"]!!.jsonPrimitive.int)
        assertEquals(5000, filterJson["offset"]!!.jsonPrimitive.int)

        // Custom column name
        filter = StorageListFilter.Files().apply { sortBy("custom_field", SortOrder.ASC) }
        filterJson = filter.buildBody()
        assertEquals("custom_field", filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
    }

    @Test
    fun testFilesFilterCombinations() {
        // Limit and offset
        var filter = StorageListFilter.Files().apply {
            limit = 20
            offset = 40
        }
        var filterJson = filter.buildBody()
        assertEquals(20, filterJson["limit"]!!.jsonPrimitive.int)
        assertEquals(40, filterJson["offset"]!!.jsonPrimitive.int)
        assertNull(filterJson["search"])

        // All parameters together
        filter = StorageListFilter.Files().apply {
            limit = 100
            offset = 200
            search = "production-files"
            sortBy("updated_at", SortOrder.DESC)
        }
        filterJson = filter.buildBody()
        assertEquals(100, filterJson["limit"]!!.jsonPrimitive.int)
        assertEquals(200, filterJson["offset"]!!.jsonPrimitive.int)
        assertEquals("production-files", filterJson["search"]!!.jsonPrimitive.content)
        assertEquals("updated_at", filterJson["sortBy"]!!.jsonObject["column"]!!.jsonPrimitive.content)
        assertEquals("desc", filterJson["sortBy"]!!.jsonObject["order"]!!.jsonPrimitive.content)
    }

}
