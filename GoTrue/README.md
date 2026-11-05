# Deprecation notice

**Starting with version 3.0.0, the module is called `auth-kt`. Checkout the updated [README](/Auth)**.

The `gotrue-kt` artifact will no longer be published after version 3.0.0.

# Supabase-kt GoTrue

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

# Installation

Newest version: [![](https://img.shields.io/github/release/supabase-community/supabase-kt?label=)](https://github.com/supabase-community/supabase-kt/releases)

Versions above 3.0.0 are available under the `auth-kt` module.

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supabase:gotrue-kt:VERSION")
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
