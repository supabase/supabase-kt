# Supabase-kt Functions

Extends Supabase-kt with a multiplatform Functions client.

Supported targets:

| Target | **JVM** | **Android** | **JS** | **Wasm** | **Apple** | **Windows** | **Linux** |
|--------|---------|-------------|--------|----------|-----------|-------------|-----------|
| Status | ✅       | ✅           | ✅      | ✅        | ✅         | ✅           | ✅         |

<details>

<summary>In-depth Kotlin targets</summary>

**JS**: Browser, NodeJS

**Wasm**: wasm-js

**Apple:**

- iOS: iosArm64, iosSimulatorArm64, iosX64

- tvOS: tvosArm64, tvosX64, tvosSimulatorArm64

- watchOS: watchosArm64, watchosDeviceArm64, watchosX64, watchosSimulatorArm64

- MacOS: macosX64, macosArm64

**Windows**: mingwX64

**Linux**: linuxX64

</details>

# Supabase Service Compatibility

`functions-kt` only guarantees compatibility with the [latest Supabase Edge Runtime release](https://github.com/supabase/edge-runtime/releases). Older Supabase Edge Runtime releases are supported on a best-effort basis, but compatibility is not guaranteed.

If you self-host the Supabase Edge Runtime, we recommend upgrading to the latest release when upgrading this SDK.

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase-community/supabase-kt?label=)](https://github.com/supabase-community/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:functions-kt:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information
```kotlin
val client = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {
    
    //...
    
    install(Functions) {
        // settings
    }
    
}
```

# Usage

See [Functions documentation](https://supabase.com/docs/reference/kotlin/functions-invoke) for usage