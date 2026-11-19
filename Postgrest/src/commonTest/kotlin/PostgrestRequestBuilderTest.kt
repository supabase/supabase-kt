import io.supabase.postgrest.query.AcceptHeader
import io.supabase.postgrest.query.Columns
import io.supabase.postgrest.query.Count
import io.supabase.postgrest.query.Order
import io.supabase.postgrest.query.PostgrestQueryBuilder
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


class PostgrestRequestBuilderTest {

    @Test
    fun testPreferNoCount() {
        testRequestBuilder(
            requestBuilder = {

            },
            httpRequestHandler = {
                assertEquals("prefer=test", headers[PostgrestQueryBuilder.HEADER_PREFER])
            }
        )
    }

    @Test
    fun testPreferCount() {
        testRequestBuilder(
            requestBuilder = {
                count(Count.EXACT)
            },
            httpRequestHandler = {
                assertEquals("count=exact,prefer=test", headers[PostgrestQueryBuilder.HEADER_PREFER])
            }
        )
    }

    @Test
    fun testPreferDryRun() {
        testRequestBuilder(
            requestBuilder = {
                dryRun()
            },
            httpRequestHandler = {
                assertEquals("tx=rollback,prefer=test", headers[PostgrestQueryBuilder.HEADER_PREFER])
            }
        )
    }

    @Test
    fun testHttpMethod() {
        testRequestBuilder(
            requestBuilder = {
                httpMethod = HttpMethod.Head
            },
            httpRequestHandler = {
                assertEquals(HttpMethod.Head, method)
            }
        )
    }

    @Test
    fun testContentType() {
        testRequestBuilder(
            requestBuilder = {

            },
            httpRequestHandler = {
                assertEquals(ContentType.Application.Json.toString(), headers[HttpHeaders.ContentType])
            }
        )
    }

    @Test
    fun testSchemaHeaderGetHead() {
        for(method in listOf(HttpMethod.Get, HttpMethod.Head)) {
            testRequestBuilder(
                requestBuilder = {
                    httpMethod = method
                },
                httpRequestHandler = {
                    assertNull(headers["Content-Profile"])
                    assertEquals("public", headers["Accept-Profile"])
                }
            )
        }
    }

    @Test
    fun testSchemaHeaderOther() {
        for(method in listOf(HttpMethod.Post, HttpMethod.Delete, HttpMethod.Patch)) {
            testRequestBuilder(
                requestBuilder = {
                    httpMethod = method
                },
                httpRequestHandler = {
                    assertNull(headers["Accept-Profile"])
                    assertEquals("public", headers["Content-Profile"])
                }
            )
        }
    }

    @Test
    fun testAcceptCsv() {
        testRequestBuilder(
            requestBuilder = {
                csv()
            },
            httpRequestHandler = {
                assertEquals("text/csv", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptGeoJson() {
        testRequestBuilder(
            requestBuilder = {
                geojson()
            },
            httpRequestHandler = {
                assertEquals("application/geo+json", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptJsonNoStrip() {
        testRequestBuilder(
            requestBuilder = {

            },
            httpRequestHandler = {
                assertEquals("application/json", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptJsonStrip() {
        testRequestBuilder(
            requestBuilder = {
                stripNulls()
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.array+json;nulls=stripped", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testStripNullsThrowsWithCsv() {
        assertFailsWith<IllegalArgumentException> {
            testRequestBuilder(
                requestBuilder = {
                    csv()
                    stripNulls()
                },
                httpRequestHandler = {}
            )
        }
    }

    @Test
    fun testAcceptSingleNoStrip() {
        testRequestBuilder(
            requestBuilder = {
                single()
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.object+json", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptSingleStrip() {
        testRequestBuilder(
            requestBuilder = {
                single()
                stripNulls()
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.object+json;nulls=stripped", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptMaybeSingleNoStrip() {
        testRequestBuilder(
            requestBuilder = {
                maybeSingle()
                assertEquals(AcceptHeader.Single(true), acceptHeader)
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.object+json", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testAcceptMaybeSingleStrip() {
        testRequestBuilder(
            requestBuilder = {
                maybeSingle()
                stripNulls()
                assertEquals(AcceptHeader.Single(true), acceptHeader)
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.object+json;nulls=stripped", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testExplainWithNoOptions() {
        testRequestBuilder(
            requestBuilder = {
                explain()
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.plan+text; for=\"application/json\"; options=;", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testExplainWithOtherMediaType() {
        testRequestBuilder(
            requestBuilder = {
                csv()
                explain()
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.plan+text; for=\"text/csv\"; options=;", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testExplainWithOptions() {
        testRequestBuilder(
            requestBuilder = {
                explain(
                    analyze = true,
                    verbose = true,
                    buffers = true,
                    settings = true,
                    wal = true,
                    format = "json"
                )
            },
            httpRequestHandler = {
                assertEquals("application/vnd.pgrst.plan+json; for=\"application/json\"; options=analyze|verbose|settings|buffers|wal;", headers[HttpHeaders.Accept])
            }
        )
    }

    @Test
    fun testOrderWithoutReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                order("messages", Order.ASCENDING, true)
            },
            httpRequestHandler = {
                assertEquals("messages.asc.nullsfirst", url.parameters["order"])
            }
        )
    }

    @Test
    fun testOrderWithoutReferencedTableNullsLast() {
        testRequestBuilder(
            requestBuilder = {
                order("messages", Order.ASCENDING, false)
            },
            httpRequestHandler = {
                assertEquals("messages.asc.nullslast", url.parameters["order"])
            }
        )
    }

    @Test
    fun testOrderWithReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                order("messages", Order.ASCENDING, true, "table")
            },
            httpRequestHandler = {
                assertEquals("messages.asc.nullsfirst", url.parameters["table.order"])
            }
        )
    }

    @Test
    fun testLimitWithoutReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                limit(10)
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["limit"])
            }
        )
    }

    @Test
    fun testLimitWithReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                limit(10, "table")
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["table.limit"])
            }
        )
    }

    @Test
    fun testRangeWithoutReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                range(10, 20)
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["offset"])
                assertEquals("11", url.parameters["limit"])
            }
        )
    }

    @Test
    fun testRangeWithReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                range(10, 20, "table")
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["table.offset"])
                assertEquals("11", url.parameters["table.limit"])
            }
        )
    }

    @Test
    fun testLongRangeWithoutReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                range(10L..20L)
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["offset"])
                assertEquals("11", url.parameters["limit"])
            }
        )
    }

    @Test
    fun testLongRangeWithReferencedTable() {
        testRequestBuilder(
            requestBuilder = {
                range(10L..20L, "table")
            },
            httpRequestHandler = {
                assertEquals("10", url.parameters["table.offset"])
                assertEquals("11", url.parameters["table.limit"])
            }
        )
    }

    @Test
    fun testSelect() {
        val myColumns = Columns.raw("id,name")
        testRequestBuilder(
            requestBuilder = {
                select(myColumns)
            },
            httpRequestHandler = {
                assertEquals("id,name", url.parameters["select"])
            }
        )
    }

    private fun testRequestBuilder(
        schema: String = "public",
        requestBuilder: MockPostgrestRequestBuilder.() -> Unit,
        httpRequestHandler: HttpRequestBuilder.() -> Unit
    ) {
        val request = postgrestRequest(defaultSchema = schema, builder = requestBuilder)
        val httpRequestBuilder = HttpRequestBuilder()
        with(request) {
            httpRequestBuilder.apply()
        }
        httpRequestHandler(httpRequestBuilder)
    }

}
