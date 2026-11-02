import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion


fun ApplicationExtension.configureApplicationAndroidTarget(
    javaVersion: JavaVersion = JavaVersion.VERSION_1_8
) {
    compileSdk = 37
    namespace = "io.github.jan.supabase.android"

    defaultConfig {
        applicationId = "io.github.jan.supabase.android"
        minSdk = 26
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}