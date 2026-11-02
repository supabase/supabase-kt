import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.compose.plugin.get().pluginId)
    alias(libs.plugins.compose.compiler)
    id("com.android.application")
    alias(libs.plugins.kotlinx.plugin.serialization)
}

group = "io.github.jan.supabase"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":sample:chat-demo-mpp:common"))
    implementation(libs.androidx.activity.compose)
}

android {
    configureApplicationAndroidTarget(JavaVersion.VERSION_11)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
    //configuredAndroidTarget()
}