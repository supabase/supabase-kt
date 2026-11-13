plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.add("io.supabase.annotations.SupabaseExperimental")
    }
}

dependencies {
    testImplementation(project(":supabase-kt"))
    testImplementation(project(":auth-kt"))
    testImplementation(project(":postgrest-kt"))
    testImplementation(project(":storage-kt"))
    testImplementation(project(":functions-kt"))
    testImplementation(project(":realtime-kt"))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    enabled = project.hasProperty("integration")
    useJUnitPlatform()
}
