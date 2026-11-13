# Troubleshooting

This page contains common problems and solutions for the supabase-kt library.

## Check debug logs

To see debug logs, you have to change the `defaultLogLevel` in the `SupabaseClientBuilder` to `LogLevel.DEBUG`:
```kotlin
val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
    defaultLogLevel = LogLevel.DEBUG
}
```
    

### Reading the logs on iOS

On iOS the library logs through the unified logging system (`os_log`),
not through stdout. Setting `defaultLogLevel = LogLevel.DEBUG` is not
enough to see them: they will not show up in the console that
`xcrun devicectl ... --console` attaches to a physical device, so it
looks as if the library is not logging at all.

**Physical device.** Launch the app with these two environment
variables, and the logs appear in the same console, including the
debug level:

```bash
DEVICECTL_CHILD_OS_ACTIVITY_DT_MODE=YES DEVICECTL_CHILD_OS_ACTIVITY_MODE=debug \
  xcrun devicectl device process launch --console --terminate-existing \
  --device <device-id> <bundle-id>
```

**Simulator.** Stream the log of the booted simulator, filtered by
your process name:

```bash
xcrun simctl spawn booted log stream --level debug --predicate 'process == "<AppName>"'
```

Remember to set the log level back before shipping.

## Frequent problems

### General

<details><summary>supabase-kt is trying to connect to "localhost"</summary>

If you are on android, make sure you add the following intent:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Also check if you are using the correct url. If you are using the Supabase hosted version, use `https://<project-id>.supabase.co` as the url. If you are using your own Supabase instance, use the url of your instance.

</details>

<details><summary>Request failed with TLS sessions are not supported on Native platform</summary>

Assuming you are on the iOS target, try to use the [Darwin](https://ktor.io/docs/http-client-engines.html#darwin) Ktor client engine.

</details>

<details><summary>[Android] Retrieving data in the background does not work</summary>

By default, if the App changes into background, the Auth plugin clears the session from memory as auto-refresh is also disabled. To disable this behavior, change the `enableLifecycleCallbacks` property in the Auth config to `false`.

Note that this will generally disable the automatic session refresh and session loading, so you have to handle this yourself.

[Here is the default implementation](https://github.com/supabase/supabase-kt/blob/2ac384478e714624bce1af9f26e019dd2f43a118/GoTrue/src/androidMain/kotlin/io/github/jan/supabase/gotrue/setupPlatform.kt#L29)

</details>

### Postgrest

<details><summary>My postgrest select request / bucket list request returns an empty list</summary>

Make sure you are using the correct table name. Also, if you have RLS enabled, make sure your user has access to the table.

</details>

<details><summary>Serializer for Class 'YourClass' not found</summary>

If you are trying to use a custom class as a parameter in a postgrest request, you have to register a serializer for that class. You can do this by adding the following line to your code:

```kotlin
@Serializable
data class YourClass(
    val id: String,
    val name: String
)
```

</details>

### Realtime

<details><summary>I get an error when I try to use the Realtime plugin: IllegalArgumentException: Engine doesn't support WebSocketCapability</summary>

Not all Ktor client [engines](https://ktor.io/docs/http-client-engines.html#limitations) support Websockets. If you are using Android, you can use OkHttp or CIO instead.

[![engines](https://user-images.githubusercontent.com/26686035/228956561-195ea7ef-a442-4e74-93c7-6aac46c0ef1c.png)](https://ktor.io/docs/http-client-engines.html#limitations)

</details>

### Auth

<details><summary>Auth#sessionStatus doesn't change when signing up</summary>

If you don't have auto confirm enabled, your user has to confirm their email/phone number before they can sign in. Once they confirm, the session status will change.
On Android and iOS, you can use deeplinking to automatically sign-in when the user clicks on the confirmation link.

</details>

<details><summary>Deeplinking successful, but there is no session in the Auth plugin</summary>

Make sure you call the `handleDeeplinks` method in your activity/fragment. This method will check if the deeplink is valid and if so, it will initialize a session.
Also make sure you specified the right **schema** and **host** in the Auth plugin.
If that doesn't help, enable logging and check for errors.

</details>

<details><summary>I wrote my own SessionManager and the restored session is incomplete or wrong</summary>

You may want your own `SessionManager` when the default storage does not fit:
to keep the token somewhere else (Keychain, encrypted preferences, DataStore),
to plug in a storage layer you already have in common code, or to keep more
than one session at the same time. The interface is three methods:
`saveSession`, `loadSession` and `deleteSession`. Pass it in the Auth config:

```kotlin
install(Auth) {
    sessionManager = MySessionManager()
}
```

Two things to know when writing one:

- **If you serialize `UserSession` with kotlinx.serialization, the `Json`
  instance needs `encodeDefaults = true`.** The default `Json` skips fields
  that hold their default value, so the saved JSON looks fine but the session
  restored on the next launch is missing fields. The built-in
  `SettingsSessionManager` uses its own `Json` with `encodeDefaults = true`
  for this reason.
- **`SettingsSessionManager` is not available in `commonMain`.** It lives in
  an intermediate source set, so you cannot extend it or reference it from
  common code; write your own against the interface, or create it from
  platform code.

If all you need is several sessions stored side by side, you do not need
your own implementation: `SettingsSessionManager` takes a `key` parameter,
so each `SupabaseClient` can keep its session under its own key.

</details>

**If your problem does not occur here, feel free to create an [issue](https://github.com/supabase/supabase-kt/issues/new/choose) or a [discussion](https://github.com/supabase/supabase-kt/discussions/new/choose).**