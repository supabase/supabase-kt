import io.github.jan.supabase.CurrentPlatformTarget
import io.github.jan.supabase.PlatformTarget
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.formUrlEncode
import io.ktor.http.parametersOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Instant

@Serializable
data class TestData(@SerialName("created_at") val createdAt: Instant)

data class KPropTest(val stringProp: String, val intProp: Int, val booleanProp: Boolean)

class PostgrestFilterBuilderTest {

    @Test
    fun filterFloat() {
        val filter = filterToString {
            eq("id", 1.1)
        }
        assertEquals("id=eq.1.1", filter)
    }

    @Test
    fun eq() {
        val filter = filterToString {
            eq("id", 1)
        }
        assertEquals("id=eq.1", filter)
    }

    @Test
    fun eq_with_reserved() {
        val filter = filterToString {
            eq("time", "2004-09-16T23:59:58.75")
        }
        assertEquals("time=eq.2004-09-16T23:59:58.75", filter)
    }

    @Test
    fun eq_with_quotes() {
        val filter = filterToString {
            eq("message", "Hello, \"World\"")
        }
        assertEquals("message=eq.Hello,+\"World\"", filter)
    }

    @Test
    fun in_quoted() {
        val filter = filterToString {
            isIn("message", listOf("\"Hello, World\"", "Goodbye.", "Greetings"))
        }
        assertEquals("message=in.(\"\\\"Hello,+World\\\"\",\"Goodbye.\",Greetings)", filter)
    }

    @Test
    fun neq() {
        val filter = filterToString {
            neq("id", 1)
        }
        assertEquals("id=neq.1", filter)
    }

    @Test
    fun gt() {
        val filter = filterToString {
            gt("id", 1)
        }
        assertEquals("id=gt.1", filter)
    }

    @Test
    fun gte() {
        val filter = filterToString {
            gte("id", 1)
        }
        assertEquals("id=gte.1", filter)
    }

    @Test
    fun lt() {
        val filter = filterToString {
            lt("id", 1)
        }
        assertEquals("id=lt.1", filter)
    }

    @Test
    fun lte() {
        val filter = filterToString {
            lte("id", 1)
        }
        assertEquals("id=lte.1", filter)
    }

    @Test
    fun like() {
        val filter = filterToString {
            like("name", "_n*")
        }
        assertEquals("name=like._n*", filter)
    }

    @Test
    fun filterNot() {
        val filter = filterToString {
            filterNot("id", FilterOperator.EQ, 1)
        }
        assertEquals("id=not.eq.1", filter)
    }

    @Test
    fun filterNotOperation() {
        val filter = filterToString {
            filterNot(FilterOperation("id", FilterOperator.EQ, 1))
        }
        assertEquals("id=not.eq.1", filter)
    }

    @Test
    fun likeAll() {
        val filter = filterToString {
            likeAll("name", listOf("a", "b"))
        }
        assertEquals("name=like(all).{a,b}", filter)
    }

    @Test
    fun likeAny() {
        val filter = filterToString {
            likeAny("name", listOf("a", "b"))
        }
        assertEquals("name=like(any).{a,b}", filter)
    }

    @Test
    fun ilikeAll() {
        val filter = filterToString {
            ilikeAll("name", listOf("a", "b"))
        }
        assertEquals("name=ilike(all).{a,b}", filter)
    }

    @Test
    fun ilikeAny() {
        val filter = filterToString {
            ilikeAny("name", listOf("a", "b"))
        }
        assertEquals("name=ilike(any).{a,b}", filter)
    }

    @Test
    fun likeAll_emptyList() {
        val filter = filterToString {
            likeAll("name", emptyList())
        }
        assertEquals("name=like(all).{}", filter)
    }

    @Test
    fun likeAny_emptyList() {
        val filter = filterToString {
            likeAny("name", emptyList())
        }
        assertEquals("name=like(any).{}", filter)
    }

    @Test
    fun rangeLte() {
        val filter = filterToString {
            rangeLte("id", 1L to 10L)
        }
        assertEquals("id=nxr.(1,10)", filter)
    }

    @Test
    fun rangeGte() {
        val filter = filterToString {
            rangeGte("id", 1L to 10L)
        }
        assertEquals("id=nxl.(1,10)", filter)
    }

    @Test
    fun rangeLt() {
        val filter = filterToString {
            rangeLt("id", 1L to 10L)
        }
        assertEquals("id=sl.(1,10)", filter)
    }

    @Test
    fun rangeGt() {
        val filter = filterToString {
            rangeGt("id", 1L to 10L)
        }
        assertEquals("id=sr.(1,10)", filter)
    }

    @Test
    fun regexMatch() {
        val filter = filterToString {
            regexMatch("name", "person")
        }
        assertEquals("name=match.person", filter)
    }

    @Test
    fun regexIMatch() {
        val filter = filterToString {
            regexIMatch("name", "person")
        }
        assertEquals("name=imatch.person", filter)
    }

    @Test
    fun match() {
        val query = buildMap {
            put("column1", 1)
            put("column2", "string")
            put("column3", 3.5f)
        }
        val filter = filterToString {
            match(query)
        }
        assertEquals("column1=eq.1&column2=eq.string&column3=eq.3.5", filter)
    }

    @Test
    fun ilike() {
        val filter = filterToString {
            ilike("name", "_n_")
        }
        assertEquals("name=ilike._n_", filter)
    }

    @Test
    fun isIn() {
        val filter = filterToString {
            isIn("id", listOf(1, 2, 3))
        }
        assertEquals("id=in.(1,2,3)", filter)
    }

    @Test
    fun notIn() {
        val filter = filterToString {
            notIn("id", listOf(1, 2, 3))
        }
        assertEquals("id=not.in.(1,2,3)", filter)
    }

    @Test
    fun exact() {
        val filter = filterToString {
            exact("id", null)
        }
        assertEquals("id=is.null", filter)
    }

    @Test
    fun contains() {
        val filter = filterToString {
            contains("name", listOf("n", "a", "m"))
        }
        assertEquals("name=cs.{n,a,m}", filter)
    }

    @Test
    fun contained() {
        val filter = filterToString {
            contained("id", listOf(1, 2, 3))
        }
        assertEquals("id=cd.{1,2,3}", filter)
    }

    @Test
    fun overlaps() {
        val filter = filterToString {
            overlaps("id", listOf(1, 2, 3))
        }
        assertEquals("id=ov.{1,2,3}", filter)
    }

    @Test
    fun contains_escaped() {
        val filter = filterToString {
            contains("tags", listOf("a,b", "c"))
        }
        assertEquals("tags=cs.{\"a,b\",c}", filter)
    }

    @Test
    fun contains_escaped_braces_quotes_and_backslashes() {
        val filter = filterToString {
            contains("tags", listOf("a{b", "c}d", "e\"f", "g\\h"))
        }
        assertEquals("tags=cs.{\"a{b\",\"c}d\",\"e\\\"f\",\"g\\\\h\"}", filter)
    }

    @Test
    fun contains_quoted_empty_and_null_lookalikes() {
        val filter = filterToString {
            contains("tags", listOf("", "NULL", "null"))
        }
        assertEquals("tags=cs.{\"\",\"NULL\",\"null\"}", filter)
    }

    @Test
    fun contains_quoted_surrounding_whitespace() {
        val filter = filterToString {
            contains("tags", listOf(" a "))
        }
        assertEquals("tags=cs.{\"+a+\"}", filter)
    }

    @Test
    fun contains_null_element() {
        val filter = filterToString {
            filter("tags", FilterOperator.CS, listOf("a", null))
        }
        assertEquals("tags=cs.{a,NULL}", filter)
    }

    @Test
    fun contained_escaped() {
        val filter = filterToString {
            contained("tags", listOf("a,b"))
        }
        assertEquals("tags=cd.{\"a,b\"}", filter)
    }

    @Test
    fun overlaps_escaped() {
        val filter = filterToString {
            overlaps("tags", listOf("a,b"))
        }
        assertEquals("tags=ov.{\"a,b\"}", filter)
    }

    @Test
    fun likeAll_escaped() {
        val filter = filterToString {
            likeAll("name", listOf("a{b", "c,d"))
        }
        assertEquals("name=like(all).{\"a{b\",\"c,d\"}", filter)
    }

    @Test
    fun likeAll_emptyPattern() {
        val filter = filterToString {
            likeAll("name", listOf(""))
        }
        assertEquals("name=like(all).{\"\"}", filter)
    }

    @Test
    fun and_or() {
        val filter = filterToString {
            and {
                eq("id", 1)
                or {
                    eq("id", 2)
                    eq("id", 3)
                }
            }
        }
        assertEquals("and=(id.eq.1,or(id.eq.2,id.eq.3))", filter)
    }

    @Test
    fun and_referenced_table() {
        val filter = filterToString {
            and(referencedTable = "shops") {
                eq("id", 1)
                eq("id", 2)
            }
        }
        assertEquals("shops.and=(id.eq.1,id.eq.2)", filter)
    }

    @Test
    fun or_referenced_table() {
        val filter = filterToString {
            or(referencedTable = "shops") {
                eq("id", 1)
                eq("id", 2)
            }
        }
        assertEquals("shops.or=(id.eq.1,id.eq.2)", filter)
    }

    @Test
    fun and_negated() {
        val filter = filterToString {
            and(negate = true) {
                eq("id", 1)
                eq("id", 2)
            }
        }
        assertEquals("not.and=(id.eq.1,id.eq.2)", filter)
    }

    @Test
    fun or_negated() {
        val filter = filterToString {
            or(negate = true) {
                eq("id", 1)
                eq("id", 2)
            }
        }
        assertEquals("not.or=(id.eq.1,id.eq.2)", filter)
    }

    @Test
    fun and_empty() {
        val filter = filterToString {
            and { }
        }
        assertEquals("", filter)
    }

    @Test
    fun or_empty() {
        val filter = filterToString {
            or { }
        }
        assertEquals("", filter)
    }

    @Test
    fun and_escaped() {
        val filter = filterToString {
            and {
                eq("id1", "foo.bar")
                eq("id2", "bar.baz")
            }
        }
        assertEquals("and=(id1.eq.\"foo.bar\",id2.eq.\"bar.baz\")", filter)
    }

    @Test
    fun sl() {
        val filter = filterToString {
            sl("id", 1L to 10L)
        }
        assertEquals("id=sl.(1,10)", filter)
    }

    @Test
    fun sr() {
        val filter = filterToString {
            sr("id", 1L to 10L)
        }
        assertEquals("id=sr.(1,10)", filter)
    }

    @Test
    fun nxl() {
        val filter = filterToString {
            nxl("id", 1L to 10L)
        }
        assertEquals("id=nxl.(1,10)", filter)
    }

    @Test
    fun nxr() {
        val filter = filterToString {
            nxr("id", 1L to 10L)
        }
        assertEquals("id=nxr.(1,10)", filter)
    }

    @Test
    fun adj() {
        val filter = filterToString {
            adjacent("id", 1L to 10L)
        }
        assertEquals("id=adj.(1,10)", filter)
    }

    @Test
    fun cs() {
        val filter = filterToString {
            cs("id", listOf(1, 2))
        }
        assertEquals("id=cs.{1,2}", filter)
    }

    @Test
    fun cd() {
        val filter = filterToString {
            cd("id", listOf(1, 2))
        }
        assertEquals("id=cd.{1,2}", filter)
    }

    @Test
    fun ov() {
        val filter = filterToString {
            ov("id", listOf(1, 2))
        }
        assertEquals("id=ov.{1,2}", filter)
    }

    @Test
    fun propEq() {
        val filter = filterToString {
            KPropTest::intProp eq 1
        }
        assertEquals("intProp=eq.1", filter)
    }

    @Test
    fun propNeq() {
        val filter = filterToString {
            KPropTest::intProp neq 1
        }
        assertEquals("intProp=neq.1", filter)
    }

    @Test
    fun propGt() {
        val filter = filterToString {
            KPropTest::intProp gt 1
        }
        assertEquals("intProp=gt.1", filter)
    }

    @Test
    fun propGte() {
        val filter = filterToString {
            KPropTest::intProp gte 1
        }
        assertEquals("intProp=gte.1", filter)
    }

    @Test
    fun propLt() {
        val filter = filterToString {
            KPropTest::intProp lt 1
        }
        assertEquals("intProp=lt.1", filter)
    }

    @Test
    fun propLte() {
        val filter = filterToString {
            KPropTest::intProp lte 1
        }
        assertEquals("intProp=lte.1", filter)
    }

    @Test
    fun propLike() {
        val filter = filterToString {
            KPropTest::stringProp like "_n*"
        }
        assertEquals("stringProp=like._n*", filter)
    }

    @Test
    fun propMatch() {
        val filter = filterToString {
            KPropTest::stringProp match "person"
        }
        assertEquals("stringProp=match.person", filter)
    }

    @Test
    fun propIlike() {
        val filter = filterToString {
            KPropTest::stringProp ilike "_n*"
        }
        assertEquals("stringProp=ilike._n*", filter)
    }

    @Test
    fun propImatch() {
        val filter = filterToString {
            KPropTest::stringProp imatch "person"
        }
        assertEquals("stringProp=imatch.person", filter)
    }

    @Test
    fun propIsExact() {
        val filter = filterToString {
            KPropTest::booleanProp isExact true
        }
        assertEquals("booleanProp=is.true", filter)
    }

    @Test
    fun propIsIn() {
        val filter = filterToString {
            KPropTest::intProp isIn listOf(1, 2)
        }
        assertEquals("intProp=in.(1,2)", filter)
    }

    @Test
    fun propRangeLt() {
        val filter = filterToString {
            KPropTest::intProp rangeLt (1 to 10)
        }
        assertEquals("intProp=sl.(1,10)", filter)
    }

    @Test
    fun propRangeLte() {
        val filter = filterToString {
            KPropTest::intProp rangeLte (1 to 10)
        }
        assertEquals("intProp=nxr.(1,10)", filter)
    }

    @Test
    fun propRangeGt() {
        val filter = filterToString {
            KPropTest::intProp rangeGt (1 to 10)
        }
        assertEquals("intProp=sr.(1,10)", filter)
    }

    @Test
    fun propRangeGte() {
        val filter = filterToString {
            KPropTest::intProp rangeGte (1 to 10)
        }
        assertEquals("intProp=nxl.(1,10)", filter)
    }

    @Test
    fun propAdjacent() {
        val filter = filterToString {
            KPropTest::intProp adjacent (1 to 10)
        }
        assertEquals("intProp=adj.(1,10)", filter)
    }

    @Test
    fun propOverlaps() {
        val filter = filterToString {
            KPropTest::intProp overlaps listOf(1, 2)
        }
        assertEquals("intProp=ov.{1,2}", filter)
    }

    @Test
    fun propContains() {
        val filter = filterToString {
            KPropTest::intProp contains listOf(1, 2)
        }
        assertEquals("intProp=cs.{1,2}", filter)
    }

    @Test
    fun propContained() {
        val filter = filterToString {
            KPropTest::intProp contained listOf(1, 2)
        }
        assertEquals("intProp=cd.{1,2}", filter)
    }

    @Test
    fun propertyConversionWithSnakeCase() {
        assertEquals("created_at", PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE(TestData::createdAt))
    }

    @Test
    fun propertyConversionWithSerialName() {
        if (CurrentPlatformTarget in listOf(PlatformTarget.JVM, PlatformTarget.ANDROID)) {
            assertEquals("created_at", PropertyConversionMethod.SERIAL_NAME(TestData::createdAt))
        } else {
            assertFails { PropertyConversionMethod.SERIAL_NAME(TestData::createdAt) }
        }
    }

    @Test
    fun textSearchPlain() {
        val filter = filterToString {
            textSearch("description", "apple", TextSearchType.PLAINTO)
        }
        assertEquals("description=plfts.apple", filter)
    }

    @Test
    fun textSearchConfig() {
        val filter = filterToString {
            textSearch("description", "apple", TextSearchType.WEBSEARCH, "english")
        }
        assertEquals("description=wfts(english).apple", filter)
    }

    @Test
    fun textSearchKeepsOtherFiltersOnTheColumn() {
        val params = PostgrestFilterBuilder(PropertyConversionMethod.NONE).apply {
            ilike("description", "%fruit%")
            textSearch("description", "apple", TextSearchType.PLAINTO)
        }.params
        assertEquals(listOf("ilike.%fruit%", "plfts.apple"), params["description"])
    }

    @Test
    fun csPair() {
        val filter = filterToString {
            filter("id", FilterOperator.CS, 1 to 2)
        }
        assertEquals("id=cs.[1,2]", filter)
    }

    @Test
    fun csScalar() {
        val filter = filterToString {
            filter("id", FilterOperator.CS, "foo")
        }
        assertEquals("id=cs.foo", filter)
    }

    @Test
    fun csScalarLogicalExpression() {
        val filter = filterToString {
            and {
                filter("id", FilterOperator.CS, "foo,bar")
            }
        }
        assertEquals("and=(id.cs.\"foo,bar\")", filter)
    }

    @Test
    fun isDistinct() {
        val filter = filterToString {
            filter("id", FilterOperator.ISDISTINCT, 1)
        }
        assertEquals("id=isdistinct.1", filter)
    }

    @Test
    fun isDistinctLogicalExpression() {
        val filter = filterToString {
            and {
                filter("id", FilterOperator.ISDISTINCT, "foo,bar")
            }
        }
        assertEquals("and=(id.isdistinct.\"foo,bar\")", filter)
    }

    @Test
    fun slList() {
        val filter = filterToString {
            filter("id", FilterOperator.SL, listOf(1, 2))
        }
        assertEquals("id=sl.(1,2)", filter)
    }

    @Test
    fun slScalar() {
        val filter = filterToString {
            filter("id", FilterOperator.SL, "foo")
        }
        assertEquals("id=sl.foo", filter)
    }

    private fun filterToString(builder: PostgrestFilterBuilder.() -> Unit): String {
        return PostgrestFilterBuilder(PropertyConversionMethod.NONE).apply(block = builder).params.mapValues { (_, value) ->
            listOf(
                value.first()
            )
        }.let {
            parametersOf(it).formUrlEncode()
        }.decodeURLQueryComponent()
    }

}