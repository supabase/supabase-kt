# Migrating from version 3.X to 4.0.0

> [!NOTE]
> 4.0.0 has not been released yet. The latest release is 3.9.0, which still uses the `io.github.jan-tennert.supabase` group and the `io.github.jan.supabase` package. This guide describes the upcoming change.

### New Maven group and package name

supabase-kt is becoming an official Supabase SDK. Starting with `4.0.0`, it is published under a new Maven group and uses a new package name:

|                | 3.X                              | 4.0.0 and later |
|----------------|----------------------------------|-----------------|
| Maven group ID | `io.github.jan-tennert.supabase` | `io.supabase`   |
| Package name   | `io.github.jan.supabase`         | `io.supabase`   |

Artifact IDs are unchanged (`auth-kt`, `postgrest-kt`, `storage-kt`, `realtime-kt`, `functions-kt`, `supabase-kt`, `bom`, ...), and so are the package names below the root (e.g. `io.github.jan.supabase.auth.providers` is now `io.supabase.auth.providers`).

**Sessions and other data stored on users' devices are not affected.** Sessions, PKCE code verifiers, and resumable upload state saved by 3.X are stored under the same keys and in the same format, and are picked up automatically by 4.0.0. Your users stay signed in after updating your app.

#### How to migrate

1. Replace the Maven group in your build files (`build.gradle.kts`, `libs.versions.toml`, ...). Upgrade **all** supabase-kt modules **and** plugins (e.g. `compose-auth`, `compose-auth-ui`, `coil3-integration`) to `4.0.0` at the same time.
   ```kotlin
   // Before
   implementation(platform("io.github.jan-tennert.supabase:bom:3.9.0"))
   implementation("io.github.jan-tennert.supabase:auth-kt")
   // After
   implementation(platform("io.supabase:bom:4.0.0"))
   implementation("io.supabase:auth-kt")
   ```
2. Replace the group ID and the package name everywhere else in your project. In IntelliJ / Android Studio use *Edit > Find > Replace in Files*:
   - `io.github.jan-tennert.supabase` → `io.supabase`
   - `io.github.jan.supabase` → `io.supabase`

   Or from the command line (macOS: use `sed -i ''`):
   ```bash
   grep -rlE 'io\.github\.jan(-tennert)?\.supabase' --include='*.kt' --include='*.kts' --include='*.gradle' --include='*.java' --include='*.toml' --include='*.properties' --include='*.xml' --include='*.pro' --include='*.txt' --include='*.swift' . \
     | xargs sed -i -E -e 's/io\.github\.jan-tennert\.supabase/io.supabase/g' -e 's/io\.github\.jan\.supabase/io.supabase/g'
   ```
3. Search your project for `io.github.jan` once more. The following places are **not** checked by the compiler and stop working silently if they still use the old name:
   - **ProGuard / R8 rules** (e.g. `-keep class io.github.jan.supabase.** { *; }`). The rules no longer match, which can cause crashes that only appear in minified release builds.
   - **`AndroidManifest.xml` overrides** of the SDK's initializers (e.g. `tools:node="remove"` on `io.github.jan.supabase.auth.SupabaseInitializer` or `io.github.jan.supabase.storage.SupabaseInitializer`). They are now `io.supabase.auth.SupabaseInitializer` and `io.supabase.storage.SupabaseInitializer`.
   - **Opt-in compiler arguments** in Gradle files (e.g. `optIn("io.github.jan.supabase.annotations.SupabaseExperimental")`).
   - **Strings and reflection** that reference SDK classes by their full name.

#### "Cannot select module because of conflict with ... Both provide capability 'io.github.jan-tennert.supabase:...'"

4.0.0 declares that it replaces the corresponding 3.X module. If a dependency (for example, a plugin or third-party library you haven't upgraded yet) still pulls in a 3.X supabase-kt module, Gradle fails with a capability conflict instead of silently bundling two copies of the SDK. The two copies use different packages, so their types don't work with each other, and if both create a client they share and refresh the same stored session.

This check relies on Gradle module metadata, so it only works in Gradle builds. Maven builds don't report the conflict.

To fix it, upgrade that dependency to a version built on supabase-kt 4.0.0. You can find it with:
```bash
./gradlew :app:dependencies | grep io.github.jan-tennert.supabase
```

# Migrating from version 2.X to 3.0.0

### Ktor 3

Starting with `3.0.0`, supabase-kt now uses Ktor 3. This brings WASM support, but projects using Ktor 2 will be incompatible.
Ktor `3.0.0-rc-1` or later has to be used.

### Rename gotrue-kt to auth-kt and rename the package name

- **The `gotrue-kt` module is no longer being published starting with version `3.0.0`. Use the new `auth-kt` module.**
- **Rename `auth-kt` package name from `io.github.jan.supabase.gotrue` to `io.github.jan.supabase.auth`**.
- Refactor SessionStatus
  - Move `SessionStatus` to its own `status` package
  - Rename `SessionStatus#LoadingFromStorage` to `SessionStatus#Initializing`
  - Rename and refactor `SessionStatus#NetworkError` to `SessionStatus#RefreshFailure(cause)`
    *Note: The cause can be either `RefreshFailureCause#NetworkError` or `RefreshFailureCause#InternalServerError`. In both cases the refreshing will be retried and the session not cleared from storage. During that time, the session is obviously not usable.*

### New plugin & Kotlin targets

- New [coil3-integration](/plugins/Coil3Integration)
- New wasm-js support for almost all plugins

### File uploading & downloading 

- Each uploading method (upload, update, uploadAsFlow ...) now has a `options` DSL. Currently you can configure three things:
1. Whether to upsert or not
2. The content type (will still be inferred like in 2.X if null)
3. Additional HTTP request configurations
   Example:
```kotlin
supabase.storage.from("test").upload("test.txt", "Hello World!".encodeToByteArray()) {
    contentType = ContentType.Text.Plain
    upsert = true
}
```
- Each downloading method (downloadPublic, downloadAuthenticated, downloadPublicAsFlow, ...) now has a `options` DSL. Currently you can only configure the image transformation
  Example:
```kotlin
supabase.storage.from("test").downloadAuthenticated("test.jpg") {
    transform {
        size(100, 100)
    }
}
```
- Uploading options such as `upsert` or `contentType` for resumable uploads are now getting cached. If an upload is resumed, the options from the initial upload will be used.

### Postgrest Changes

- Move all optional function parameters for `PostgrestQueryBuilder#select()`, `insert()`, `upsert()` and `Postgrest#rpc()` to the request DSL
  Example:
```kotlin
supabase.from("table").upsert(myValue) {
    defaultToNull = false
    ignoreDuplicates = false
}
```
- Move the non-parameter variant of `Postgrest#rpc()` to the `Postgrest` interface. It was an extension function before
- Add a non-generic parameter variant of `Postgrest#rpc()` to the `Postgrest` interface. This function will be called from the existing generic variant

### Realtime changes

- `RealtimeChannel#presenceChangeFlow` is now a member function of `RealtimeChannel`. (It was an extension function before)

### Apollo GraphQL plugin

The Apollo GraphQL plugin now uses Apollo GraphQL 4.0.0.

<details>
<summary>Migrating from version 1.4.X to 2.0.0</summary>

# Migrating from version 1.4.X to 2.0.0

## GoTrue

The GoTrue module had a lot of changes including many renames:
- Rename `GoTrue` plugin to `Auth`
- Rename `GoTrueConfig` to `AuthConfig`
- Rename `SupabaseClient#gotrue` to `SupabaseClient#auth`
- Rename `Auth#loginWith` to `Auth#signInWith`
- Rename `Auth#logout` to `Auth#signOut`
- Rename `LogoutScope` to `SignOutScope`
- Rename `AdminUserUpdateBuilder#phoneNumber` to `AdminUserUpdateBuilder#phone`
- Rename `UserUpdateBuilder#phoneNumber` to `UserUpdateBuilder#phone`
- Rename `Phone.Config#phoneNumber` to `Phone.Config#phone`
- Rename `Auth#sendRecoveryEmail` to `Auth#resetPasswordForEmail`

#### The process of signing in with an OTP & SSO has also been refactored. 
#### There is now a new `OTP` Auth Provider which does the same as the old `sendOtpTo`:

Old:
```kotlin
supabase.gotrue.sendOtpTo(Email) {
    email = "example@email.com"
}
//or
supabase.gotrue.sendOtpTo(Phone) {
    phoneNumber = "+123456789"
}
```

New:
```kotlin
supabase.auth.signInWith(OTP) {
    email = "example@email.com"
    //or
    phone = "+123456789"
}
```

#### Similarly, the SSO Auth Provider was also refactored to match the other Auth Providers:

Old:
```kotlin
supabase.gotrue.loginWith(SSO.withProvider("provider"))
//or
supabase.gotrue.loginWith(SSO.withDomain("domain"))
```

New:
```kotlin
supabase.auth.signInWith(SSO) {
    providerId = "providerId"
    //or
    domain = "domain"
}
```

## Realtime

The Realtime module also had a few renames:
- Rename `Realtime#createChannel` to `Realtime#channel`
- Remove `RealtimeChannel#join` and add new `RealtimeChannel#subscribe` method, which does the same but also connects to the realtime websocket automatically
- Add `Realtime.Config#connectOnSubscribe` to disable this behaviour
- Rename `RealtimeChannel#leave` to `RealtimeChannel#unsubscribe`
- Add `SupabaseClient#channel` extension function delegating to `Realtime#channel`
- Rename `Realtime.Status` to reflect the new methods:
    - `UNSUBSCRIBED`
    - `SUBSCRIBING`
    - `SUBSCRIBED`
    - `UNSUBSCRIBING`

## Postgrest

The syntax for interacting with the PostgREST API has been refactored significantly. Each database method (`SELECT`, `UPDATE`, etc.)
now have a new builder and most of the properties which were a method parameter are now in this builder.

The filters now get applied within a `filter {}` block.

### **Examples:**

**Select**

Old:
```kotlin
supabase.postgrest.from("countries").select(count = Count.EXACT) {
    eq("id", 1)
}
```

New:
```kotlin
supabase.postgrest.from("countries").select {
    count(Count.EXACT)
    filter {
        eq("id", 1)
    }
}
```

**Insert**

Old:
```kotlin
supabase.postgrest.from("countries").update(country, returning = Returning.REPRESENTATION) { //Returning is representation by default
    eq("id", 1)
}
```

New:
```kotlin
supabase.postgrest.from("countries").update(country) {
    select() //Without this the "returning" parameter is `MINIMAL`, meaning you will not receive the data. 
    filter {
        eq("id", 1)
    }
}
```

The same applies for all other database methods. Additionally, new methods have been added to this builder:

Example:
```kotlin
val result = supabase.postgrest["messages"].select {
     single() //receive an object rather than an array
     count(Count.EXACT) //receive amount of database entries
     limit(10) //limit amount of results
     range(2, 3) //change range of results
     select() //return the data when updating/deleting/upserting (same as settings 'returning' to REPRESENTATION before)
     csv() //Receive the data as csv
     geojson() //Receive the data as geojson
     explain(/* */) //Debug queries
     filter {
          eq("id", 1)
     }
}
```

## Compose Auth

Compose Auth also had some renames:
- Rename `ComposeAuth#rememberLoginWithGoogle` to `ComposeAuth#rememberSignInWithGoogle`
- Rename `ComposeAuth#rememberLoginWithApple` to `ComposeAuth#rememberSignInWithApple`
- Rename `ComposeAuth#rememberSignOut` to `ComposeAuth#rememberSignOutWithGoogle`

Additionally, Native Google Auth on Android will now use the Credential Manager for Android 14+ devices once again.

</details>