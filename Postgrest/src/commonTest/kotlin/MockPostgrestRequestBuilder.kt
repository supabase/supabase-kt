import io.supabase.postgrest.PropertyConversionMethod
import io.supabase.postgrest.query.PostgrestRequestBuilder

class MockPostgrestRequestBuilder(defaultSchema: String, propertyConversionMethod: PropertyConversionMethod): PostgrestRequestBuilder(
    defaultSchema,
    propertyConversionMethod
) {
    override fun customPrefer(): List<String> {
        return listOf("prefer=test")
    }
}

internal inline fun postgrestRequest(
    defaultSchema: String = "public",
    propertyConversionMethod: PropertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE,
    builder: MockPostgrestRequestBuilder.() -> Unit
): MockPostgrestRequestBuilder {
    return MockPostgrestRequestBuilder(defaultSchema, propertyConversionMethod).apply(builder)
}