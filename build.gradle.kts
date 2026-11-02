@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

val excludedModules = listOf("plugins", "serializers", "test-common", "integration-test")

private val libraryFilter = { withFilter: Boolean ->
    allprojects.filter { it.name !in excludedModules && !it.path.contains("sample") && if(withFilter) true else it.name != "bom" && it.name != it.rootProject.name }
}

fun libraryModules(withBom: Boolean = true, init: Project.() -> Unit) = configure(
    libraryFilter(withBom),
    init
)

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId) apply false
    id(libs.plugins.android.kotlin.multiplatform.library.get().pluginId) apply false
    id(libs.plugins.detekt.get().pluginId) apply false
    id(libs.plugins.dokka.get().pluginId)
    alias(libs.plugins.kotlinx.plugin.serialization) apply false
    id(libs.plugins.maven.publish.get().pluginId) apply false
    id(libs.plugins.power.assert.get().pluginId) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    libraryFilter(false).forEach {
        dokka(project(it.path))
    }
}

libraryModules {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.vanniktech.maven.publish")

    group = extra["base-group"].toString()
    version = supabaseVersion

    applyPublishing()
}

val reportMerge = tasks.register<io.gitlab.arturbosch.detekt.report.ReportMergeTask>("reportMerge") {
    description = "Merges all detekt reports into a single file"
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

libraryModules(false) {
    applyDokkaWithConfiguration()
    applyPowerAssertConfiguration()
    applyDetektWithConfiguration(reportMerge)
}

tasks.register("detektAll") {
    description = "Runs detekt on all modules"
    libraryModules(false) {
        this@register.dependsOn(tasks.withType<io.gitlab.arturbosch.detekt.Detekt>())
    }
}

// Configure Gradle Task to build all sample submodules at once
configure(allprojects.filter { it.parent?.name == "sample" }) {
    val children = this.childProjects
    this.tasks.register("buildAll") {
        children.values.forEach { child ->
            this.dependsOn(child.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>())
        }
    }
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReportProperty =
        YarnLockMismatchReport.WARNING
    rootProject.the<YarnRootExtension>().reportNewYarnLockProperty = false
    rootProject.the<YarnRootExtension>().yarnLockAutoReplaceProperty = true
}