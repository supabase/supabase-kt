@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
    id(libs.plugins.compose.plugin.get().pluginId)
    alias(libs.plugins.compose.compiler)
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlinx.plugin.serialization)
}

group = "io.github.jan.supabase"
version = "1.0-SNAPSHOT"

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvmToolchain(11)
    configuredAndroidTarget()
    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    js(IR) {
        browser()
    }
    applyDefaultHierarchyTemplate {
        common {
            group("nonJs") {
                withJvm()
                withAndroidTarget()
            }
        }
    }
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.materialIconsExtended)
                addModules(SupabaseModule.AUTH)
                api(libs.koin.core)
            }
        }
        val nonJsMain by getting {
            dependencies {
                api(libs.ktor.client.cio)
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.androidx.compat)
                api(libs.androidx.core)
                api(libs.koin.android)
                api(libs.androidx.lifecycle.viewmodel.ktx)
                api(libs.androidx.lifecycle.viewmodel.compose)
                api(libs.coil2.svg)
                api(libs.coil2.compose)
                api(libs.coil2)
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val jsMain by getting {
            dependencies {
                api(libs.ktor.client.js)
            }
        }
    }
}

