# Supabase-kt Realtime

Extends Supabase-kt with a multiplatform Realtime client.

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

- watchOS: watchosArm64, watchosX64, watchosSimulatorArm64

- MacOS: macosX64, macosArm64

**Windows**: mingwX64

**Linux**: linuxX64

</details>

# Supabase Service Compatibility

`realtime-kt` only guarantees compatibility with the [latest Supabase Realtime release](https://github.com/supabase/realtime/releases). Older Supabase Realtime releases are supported on a best-effort basis, but compatibility is not guaranteed.

If you self-host Supabase Realtime, we recommend upgrading to the latest release when upgrading this SDK.

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase/supabase-kt?label=)](https://github.com/supabase/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:realtime-kt:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {

    //...

    install(Realtime) {
        // settings
    }

}
```

# Usage

See [Realtime documentation](https://supabase.com/docs/reference/kotlin/subscribe) for usage
