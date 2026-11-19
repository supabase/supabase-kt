# Supabase-kt Storage

Extends Supabase-kt with a multiplatform Storage client.

Supported targets:

| Target | **JVM** | **Android** | **JS** | **Wasm** | **Apple** | **Windows** | **Linux** |
|--------|---------|-------------|--------|----------|-----------|-------------|-----------|
| Status | ✅       | ✅           | ✅      | ✅        | ✅         | ✅           | ✅         |

> Linux has no support for persistent resumable upload url caching.

<details>

<summary>In-depth Kotlin targets</summary>

**JS**: Browser, NodeJS

**Wasm**: wasm-js

**Apple:**

- iOS: iosArm64, iosSimulatorArm64, iosX64

- tvOS: tvosArm64, tvosX64, tvosSimulatorArm64

- watchOS: watchosArm64, watchosX64, watchosSimulatorArm64

- MacOS: macosX64, macosArm64

**Windows**: mingwX64

**Linux**: linuxX64

</details>

# Supabase Service Compatibility

`storage-kt` only guarantees compatibility with the [latest Supabase Storage release](https://github.com/supabase/storage/releases). Older Supabase Storage releases are supported on a best-effort basis, but compatibility is not guaranteed.

If you self-host Supabase Storage, we recommend upgrading to the latest release when upgrading this SDK.

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase/supabase-kt?label=)](https://github.com/supabase/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:storage-kt:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {

    //...

    install(Storage) {
        // settings
    }

}
```

# Usage

See [Storage documentation](https://supabase.com/docs/reference/kotlin/storage-createbucket) for usage
