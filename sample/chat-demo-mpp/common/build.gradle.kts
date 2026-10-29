@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.compose.plugin.get().pluginId)
    alias(libs.plugins.compose.compiler)
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlinx.plugin.serialization)
}

group = "io.github.jan.supabase"
version = "1.0-SNAPSHOT"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withAndroidTarget()
            }
        }
    }
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    androidTarget()
    jvmToolchain(11)
    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    js(IR) {
        browser()
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "common"
            isStatic = true
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.materialIconsExtended)
                addModules(SupabaseModule.AUTH, SupabaseModule.POSTGREST, SupabaseModule.REALTIME, SupabaseModule.FUNCTIONS)
                api("io.github.jan-tennert.supabase:compose-auth:$pluginVersion")
                api("io.github.jan-tennert.supabase:compose-auth-ui:$pluginVersion")

                api(libs.compose.auth)
                api(libs.compose.auth.ui)
                api(libs.koin.core)
            }
        }
        val jvmAndAndroidMain by getting {
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
        val iosMain by getting {
            dependencies {
                api(libs.ktor.client.ios)
            }
        }
    }
}

configureLibraryAndroidTarget("io.github.jan.supabase.common", 26, JavaVersion.VERSION_11)
