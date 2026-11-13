import com.vanniktech.maven.publish.MavenPublishBaseExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra

val Project.supabaseVersion get() = System.getProperty("SupabaseVersion") ?: extra["supabase-version"].toString()

/**
 * The Maven group used up to 3.x
 */
private const val LEGACY_GROUP = "io.github.jan-tennert.supabase"

/**
 * Makes every published variant also provide the capability of its 3.x counterpart
 * (e.g. `io.supabase:auth-kt-jvm` also provides `io.github.jan-tennert.supabase:auth-kt-jvm`).
 *
 * If a consumer ends up with both 3.x and 4.x (e.g. through an outdated plugin), Gradle fails with a capability conflict
 * instead of silently bundling two SDK copies, which would refresh the same stored session independently.
 *
 * This only touches the published Gradle module metadata, so project dependencies within this build are unaffected.
 */
fun Project.declareLegacyCapabilities() {
    tasks.withType(GenerateModuleMetadata::class.java).configureEach {
        val metadataFile = outputFile
        // Platform modules (e.g. auth-kt-jvm) reference the root module in their "component" block, so the actual coordinates come from the publication
        val coordinates = publication.map { (it as MavenPublication).run { Triple(groupId, artifactId, version) } }
        inputs.property("legacyCapabilityCoordinates", coordinates.map { it.toList().joinToString(":") })
        doLast {
            val (group, artifactId, version) = coordinates.get()
            val file = metadataFile.get().asFile
            @Suppress("UNCHECKED_CAST")
            val metadata = JsonSlurper().parse(file) as MutableMap<String, Any?>
            @Suppress("UNCHECKED_CAST")
            (metadata["variants"] as List<MutableMap<String, Any?>>)
                .filter { "available-at" !in it } // redirects to platform modules, which declare the capability themselves
                .forEach { variant ->
                    @Suppress("UNCHECKED_CAST")
                    val capabilities = (variant["capabilities"] as List<Map<String, Any?>>?)
                        // Declaring any capability drops the implicit one, so it has to be declared explicitly
                        ?: listOf(mapOf("group" to group, "name" to artifactId, "version" to version))
                    variant["capabilities"] = capabilities + mapOf("group" to LEGACY_GROUP, "name" to artifactId, "version" to version)
                }
            file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(metadata)))
        }
    }
}

fun Project.applyPublishing() {
    extensions.configure(MavenPublishBaseExtension::class) {
        publishToMavenCentral()

        if (System.getProperty("DisableSigning") != "true") {
            signAllPublications()
        }
        coordinates(extra["base-group"].toString(), this@applyPublishing.name, supabaseVersion)

        pom {
            name.set(this@applyPublishing.name)
            description.set(this@applyPublishing.description ?: "A Kotlin Multiplatform Supabase SDK")
            inceptionYear.set("2024")
            url.set("https://github.com/supabase/supabase-kt/")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/supabase/supabase-kt/blob/master/LICENSE")
                    distribution.set("https://github.com/supabase/supabase-kt/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("TheRealJan")
                    name.set("Jan Tennert")
                    url.set("https://github.com/jan-tennert/")
                }
            }
            scm {
                url.set("https://github.com/supabase/supabase-kt/")
                connection.set("scm:git:git://github.com/supabase/supabase-kt.git")
                developerConnection.set("scm:git:ssh://git@github.com/supabase/supabase-kt.git")
            }
        }
    }
}