import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension
import java.net.URI

fun Project.applyDokkaWithConfiguration() {
    extensions.configure(DokkaExtension::class) {
        val name = when(moduleName.get()) {
            "supabase-kt" -> "Supabase"
            "functions-kt" -> "Functions"
            "auth-kt" -> "Auth"
            "postgrest-kt" -> "Postgrest"
            "realtime-kt" -> "Realtime"
            "storage-kt" -> "Storage"
            "apollo-graphql" -> "plugins/ApolloGraphQL"
            "compose-auth" -> "plugins/ComposeAuth"
            "compose-auth-ui" -> "plugins/ComposeAuthUI"
            "coil-integration" -> "plugins/CoilIntegration"
            "coil3-integration" -> "plugins/Coil3Integration"
            "imageloader-integration" -> "plugins/ImageLoaderIntegration"
            "serializer-moshi" -> "serializers/Moshi"
            "serializer-jackson" -> "serializers/Jackson"
            else -> ""
        }
        dokkaSourceSets.configureEach {
      //      includes.from("README.md")
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URI("https://github.com/supabase/supabase-kt/tree/master/$name/src"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}