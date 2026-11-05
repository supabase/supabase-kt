# Supabase-kt Auth

**Only available for versions 3.0.0 and above. For versions below 3.0.0, checkout the [old README](/GoTrue)**

Extends Supabase-kt with a multiplatform Auth client.

Supported targets:

| Target | **JVM** | **Android** | **JS** | **Wasm** | **Apple** | **Windows** | **Linux** |
|--------|---------|-------------|--------|----------|-----------|-------------|-----------|
| Status | ✅       | ✅           | ✅      | ✅        | ☑️*       | ☑️          | ☑️        |

> ☑️ = No built-in OAuth support. Linux has no support for persistent session storage.

\* **iOS and macOS are fully supported**

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

`auth-kt` only guarantees compatibility with the [latest Supabase Auth release](https://github.com/supabase/auth/releases). Older Supabase Auth releases are supported on a best-effort basis, but compatibility is not guaranteed.

If you self-host Supabase Auth, we recommend upgrading to the latest release when upgrading this SDK.

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase-community/supabase-kt?label=)](https://github.com/supabase-community/supabase-kt/releases)

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:auth-kt:VERSION")
}
```

Install the plugin in your SupabaseClient. See the [documentation](https://supabase.com/docs/reference/kotlin/initializing) for more information

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://id.supabase.co",
    supabaseKey = "apikey"
) {

    //...

    install(Auth) {
        // settings
    }

}
```

# Usage

See [Auth documentation](https://supabase.com/docs/reference/kotlin/auth-signup) for usage
