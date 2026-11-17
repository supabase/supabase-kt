package io.supabase.testing

import io.ktor.http.HttpMethod
import kotlin.test.assertEquals

fun assertMethodIs(expected: HttpMethod, actual: HttpMethod) {
    assertEquals(expected, actual, "Method should be $expected")
}

fun assertPathIs(expected: String, actual: String) {
    assertEquals(expected, actual, "Path should be '$expected'")
}