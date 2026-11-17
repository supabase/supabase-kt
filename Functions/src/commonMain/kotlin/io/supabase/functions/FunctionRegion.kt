@file:Suppress("UndocumentedPublicProperty")
package io.supabase.functions

/**
 * The region where the function is invoked.
 * @param value The value of the region
 */
enum class FunctionRegion(val value: String) {
    ANY("any"),
    AP_NORTHEAST_1("ap-northeast-1"),
    AP_NORTHEAST_2("ap-northeast-2"),
    AP_SOUTH_1("ap-south-1"),
    AP_SOUTHEAST_1("ap-southeast-1"),
    AP_SOUTHEAST_2("ap-southeast-2"),
    CA_CENTRAL_1("ca-central-1"),
    EU_CENTRAL_1("eu-central-1"),
    EU_WEST_1("eu-west-1"),
    EU_WEST_2("eu-west-2"),
    EU_WEST_3("eu-west-3"),
    SA_EAST_1("sa-east-1"),
    US_EAST_1("us-east-1"),
    US_WEST_1("us-west-1"),
    US_WEST_2("us-west-2"),
}