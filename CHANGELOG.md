# Changelog

### 3.9.0 - October 27, 2026

### Chore

* Update CODEOWNERS by @safaorhan in https://github.com/supabase/supabase-kt/pull/1435
* Update CONTRIBUTING.md with information on how to run tests by @cmelchior in https://github.com/supabase/supabase-kt/pull/1392
* Add more articles and sample by @hieuwu in https://github.com/supabase/supabase-kt/pull/1401
* docs: how to read the library logs on iOS by @veropduro in https://github.com/supabase/supabase-kt/pull/1408
* docs: Add migration to official notice to README.md. by @safaorhan in https://github.com/supabase/supabase-kt/pull/1431

### Auth

* **Add lifecycle callbacks for iOS and tvOS** by @olipetrovic in https://github.com/supabase/supabase-kt/pull/1390
  Uses the same config property as the Android implementation.
* fix(auth): [Security] Avoid writing session credentials to the platform log & mask sensitive values by @hieuwu in https://github.com/supabase/supabase-kt/pull/1391
* Handle OAuth query errors in the implicit flow on Android, Apple and web by @AtharvaLakhe in https://github.com/supabase/supabase-kt/pull/1426
* docs: notes for writing a custom SessionManager by @veropduro in https://github.com/supabase/supabase-kt/pull/1409
* Add Storage integration tests, fix vector and analytics bucket response deserialization  by @hieuwu in https://github.com/supabase/supabase-kt/pull/1410
* fix(auth): decode URL fragment parameters and recognize errors without error_code by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1424

### Storage

* feat(storage): **add new storage exception and expose `code` parameter** by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1376
  All Storage methods no longer throw a generic exception, but a StorageRestException
* fix(storage): keep the file size a Long when sizing an upload chunk by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1404
* fix(storage): fix reading resumable uploads back from the cache by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1405
* fix(storage): keep the error and message when an error body has no code by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1407
* fix(storage): download variant with channel doesn't throw rest exceptions by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1419
* fix(storage): return the unencoded path in FileUploadResponse by @RaphaelFakhri in https://github.com/supabase/supabase-kt/pull/1428

### Postgrest

* docs(postgrest): document the empty list behaviour of likeAll, likeAny, ilikeAll and ilikeAny by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1432
* fix(postgrest): escape reserved characters in array filter values by @AndroidPoet in https://github.com/supabase/supabase-kt/pull/1403
* fix(postgrest): allow multiple filters on one column by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1420

### Realtime (reliability)

* fix(realtime): handle channel errors and rejoins like realtime-js by @sproctor in https://github.com/supabase/supabase-kt/pull/1433
* fix(realtime): include join_ref on leave and access_token messages by @grdsdev in https://github.com/supabase/supabase-kt/pull/1402
* fix(realtime): prevent heartbeat tick from racing disconnect() by @alperen-kaplan in https://github.com/supabase/supabase-kt/pull/1395
* fix(realtime): reset the heartbeat ref when a new socket connects by @devjn in https://github.com/supabase/supabase-kt/pull/1423
* fix(realtime): Fix race condition when scheduling multiple disconnects by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1418
* fix(realtime): Fix race condition when rejoining channels before auth is ready by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1417
* fix(realtime): Fix race condition when resubscribing channel by @jan-tennert in https://github.com/supabase/supabase-kt/pull/1415
* fix(realtime): don't take channel messages for heartbeat replies by @sproctor in https://github.com/supabase/supabase-kt/pull/1434

### 3.8.0 - August 26, 2026

### Core

* fix(sample): move "Forgot password?" button out of overlapping Box in… by @MohammedThalha2002 in https://github.com/supabase-community/supabase-kt/pull/1367
* docs: fix broken star history chart by @OctoBored in https://github.com/supabase-community/supabase-kt/pull/1381

### Postgrest

* feat(postgrest): add dryRun(), maybeSingle() modifiers, notIn filter by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1365

### Auth
* feat(auth): add support for OAuth consent flow by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1235
* Add tests for RestException and plugin-specific subtypes by @MohammedThalha2002 in https://github.com/supabase-community/supabase-kt/pull/1368
* fix(auth): include the status code description as the fallback by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1379
* fix(auth): catch all throwables instead of exceptions for refresh and loading by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1383

### Realtime
* feat(realtime): add system flow, `replicationReady` option by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1366
```kotlin
val channel = supabase.realtime.channel("test") {
    broadcast { 
        replicationReady = true
    }
}
channel.systemFlow().collect { 
    println(it.status)
}
```
* feat(realtime): add realtime filter builder by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1380
```kotlin
channel.postgresChangeFlow<PostgresAction>("public") {
    table = "myTable"
    filter { 
        lt("id", 20)
        eq("name", "Tom")
    }
}
```
// supported by all realtime postgres methods including:
```kotlin
supabase.from("myTable").selectAsFlow(...) {
    eq("id", 2)
    lt("age", 58)
}
```
* fix(realtime): catch exceptions when resolving access token by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1386

### Storage

* feat(storage): add purge methods for buckets and single objects by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1375
```kotlin
supabase.storage.purgeBucketCache("myBucket")
supabase.storage["myBucket"].purgeCache("myFile.png")
```

### 3.7.0 - July 20, 2026

### Core

* Fix append tag by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1284
* Add Star History section to README by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1290
* Fix chat-demo-mpp example not compiling by @cmelchior in https://github.com/supabase-community/supabase-kt/pull/1296
* fix(integration-test): grant table privileges on test_items to PostgREST roles by @PierreVieira in https://github.com/supabase-community/supabase-kt/pull/1333
* implement structured X-Client-Info header by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1300
* add sdk compliance by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1354
* feat(core): check api key format when creating supabase client by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1362

### Auth

* **Add support for custom OAuth provider** by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1236
* feat(auth): expose public getter for AuthConfig.httpCallbackConfig on Desktop by @PierreVieira in https://github.com/supabase-community/supabase-kt/pull/1322
* fix(auth): don't throw an error when there is no stored session by @PierreVieira in https://github.com/supabase-community/supabase-kt/pull/1320
* **Add API support for Passkeys** by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1327 under `supabase.auth.passkeys` - Note, there is currently no sign-in method. We are still deciding how to move on with Native APIs. Compose Auth support may come as a temporary solution.
* Add PKCE support for resend by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1337
* fix(auth): clear session on any rest exception by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1359
* fix(auth): return refreshed token from resolveAccessToken + tolerate non-object error bodies by @Merkost in https://github.com/supabase-community/supabase-kt/pull/1352
* add `custom_claims_allowlist` to custom providers admin api by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1349

### Realtime

* **Add support for binary broadcasts, httpSend and VSN 2.0.0** by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1325

  **Either use the `ByteArray` type in broadcastFlow:**
  ```kotlin
  channel.broadcastFlow<ByteArray>("test").collect {
      println(it.decodeToString())
  }
  ```
  **Or use `broadcastFlow` without a type to receive both types:**
  ```kotlin
  channel.broadcastFlow("test").collect {
      when(it.payload) {
          is BroadcastPayload.Binary -> println(it.payload.data.decodeToString())
          is BroadcastPayload.Json -> println(it.payload.value)
      }
  }
  ```
  **New method to broadcast messages (the old ones are using this in the implementation if not connected)'**:
  ```kotlin
  channel.httpSend("myEvent", BroadcastPayload.Binary(byteArrayOf(1,2,3)))
  ```
* Implement deferred disconnect after removing last channel  by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1276
* docs(realtime): clarify that maxAttempts governs channel rejoin attempts by @PierreVieira in https://github.com/supabase-community/supabase-kt/pull/1331
* fix(realtime): log transient websocket reconnects at WARN instead of ERROR by @PierreVieira in https://github.com/supabase-community/supabase-kt/pull/1329

### Postgrest

* Improve test coverage in Postgrest by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1275

### Storage

* test(storage): add download-as-flow tests by @Ehsanul-Hoque in https://github.com/supabase-community/supabase-kt/pull/1317

### Functions

* fix(functions): forbid using new api key as fallback by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1360

### 3.6.0 - April 27, 2026

### Core

* Fix JS test by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1255
* Use inject-based logging approach for configurability and mocking by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1254 - *You can now provide a custom `SupabaseLoggingProcessor` via `SupabaseClientBuilder#defaultLoggingFactory`, and also override per plugin. This processor is used to actually log the messages*


### Realtime

* Fix consecutive connects by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1248

### Storage

* fix(storage): preserve full nested path in FileUploadResponse by @Sirelon in https://github.com/supabase-community/supabase-kt/pull/1239
* Fix flaky testUnauthenticatedUploadDenied in CI by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1240

### Auth

* fix(auth): Use configured httpPort instead of random port for OAuth callback server by @CristianMG in https://github.com/supabase-community/supabase-kt/pull/1237
* feat: Add support for OAuth 2.1 client admin API by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1222
* Deprecate data property on IDToken.Config by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1249
* fix(auth): don't emit spurious NotAuthenticated on Android lifecycle onStart by @alexeysfh in https://github.com/supabase-community/supabase-kt/pull/1267

### Postgrest

* feat: Add automatic retries for transient Postgrest errors by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1246
* fix: expand Cloudflare error codes in retryable status codes by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1262
* Refactor PostgrestRequests and add `stripNulls()` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1264

### Storage

* Add url options to `download`, `publicUrl`, `createSignedUrl`... by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1263

### 3.5.0 - April 02, 2026

# Changes

### Core

* Fix indentation on error lines by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1184
* Separate sub-APIs from parent APIs by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1216
* Add integration testing infrastructure with local Supabase by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1217
* Use new unix serializer for timestamps by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1215
* Documentation improvements by @jan-tennert in https://github.com/supabase/supabase/pull/44141

### Storage

* [Fix] Use options parameter in createOrContinueUpload by @vojta-horanek in https://github.com/supabase-community/supabase-kt/pull/1188
* Add experimental support for vector, analytic buckets & storage, auth improvements by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1196
* do not rewrite signed URL to render endpoint for empty transform object by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1212

### Postgrest

* Add `urlLengthLimit` and `timeout` option to postgrest by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1190
* Rename regex match method and add match method by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1213

### Auth

* Add `currentPassword` to `UserUpdateBuilder` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1211
* Fix flaky JS browser test timeout in testLoadingSessionFromStorage by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1233

### Functions

* Add streaming support for Edge Function responses by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1228

### Realtime

* Add presenceState() query API to RealtimeChannel by @thagikura in https://github.com/supabase-community/supabase-kt/pull/1221

### Compose Auth

Because I forgot to put it in the previous changelog:
- The credentials when successfully signing in via native auth are now exposed, so you can access data like name, phone number, etc. This is currently only supported for Native Google Auth on Android and Native Apple Auth on iOS:
```kotlin
is NativeSignInResult.Success -> {
    val credential = result.data.google.credential
    credential.displayName 
    credential.phoneNumber
    // ...
}
```

### 3.4.1 - February 28, 2026

### Core

* Actually fix changing the log level by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1182

### Realtime

* don't teardown channels on reconnect by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1186

### 3.4.0 - February 25, 2026

### Core

* Allow nullable types in SupabaseSerializer and Postgrest decoding by @ferhatwi in https://github.com/supabase-community/supabase-kt/pull/1175
* Switch log level for network issue related logs to DEBUG by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1159
* Temporary fix for the log level by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1169

### Auth

* Add redirectUrl to resendEmail by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1138
* Add X provider by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1140
* Add missing properties to `UserInfo` and `UserMFAFactor` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1139
* Add further debug information to requests with an expired token by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1149
* Add channel parameter to OTP provider for WhatsApp support by @JOsacky in https://github.com/supabase-community/supabase-kt/pull/1158
* Add `Auth#getClaims()` API and migrate to KotlinX IO by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1141

### Functions

* Fix invalid default rest exception in `Functions#parseErrorResponse()` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1142

### Storage

* Add `setHeader()` to BucketApi by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1177

### Realtime

* Teardown channels after disconnecting from realtime by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1178

### 3.3.0 - January 4, 2026

### Core

* Update Kotlin to version `3.3.0`
* fix: Make OS version optional in headers by @rafsanjani in https://github.com/supabase-community/supabase-kt/pull/1088
* Implement Native Google OAuth iOS in sample by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1093
* Mask sensitive information in `RestException` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1113
* Migrate to new Multiplatform Android Plugin by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1013
* Change `body()` calls to `safeBody()` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1131

### Auth

* Refactor platform specific auth initialization by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1100
* Add session failsafe and option to require a session for requests by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1071
  - When trying to make a request with an expired access token, the client will try to force-refresh the session
  - There is a new option for all plugins to require the user to be authenticated to make requests with that plugin (instead of falling back to the api key as the access token). Note that some methods ignore this option.
* Add `onError` parameter to `handleDeeplink` methods by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1136
  This is meant to handle exceptions happening when verifying the PKCE code contained in the deeplink
* Handle secure email changes in `Auth#verify` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1130
  `Auth#verifyEmailOtp()` will now return an `OtpVerifyResult`, depending on if the response contains a session.
* Expose `Auth#defaultRedirectUrl()` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1134
* Fix infinite recursion for expired sessions by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1133
* Fix a bug causing the sessionStatus to be set earlier than loading from storage by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1128
* Fix `redirect_to` url parameter for `resetPasswordForEmail()` and `getOAuthUrl()` values not being encoded by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1125

### Realtime

* Classify events with error status code as error events by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1123

### Compose Auth

- **Implement Native Google OAuth for Apple targets** by @hieuwu in https://github.com/supabase-community/supabase-kt-plugins/pull/45 also big thanks to @dosier!
  Checkout [the README](https://github.com/supabase-community/supabase-kt-plugins/tree/main/ComposeAuth#native-google-auth-on-ios) for more information

### Postgrest

* only escape filters inside logical expressions and `in` lists by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1090

### Storage

* Add support for bucket pagination and sorting by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1087
### 3.2.6 - October 29, 2025

- Update Kotlin to version `2.2.21`

### Postgrest

- Do not escape numbers in Postgrest & escape `filterNot` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1084

### 3.2.5 - October 15, 2025

### Auth

* Fix a bug causing auto-refresh to continue in the background (Android) #1060 by @jozefv-git in https://github.com/supabase-community/supabase-kt/pull/1062
* Fix `Auth#linkIdentityWithIdToken()` not working correctly by @jan-tennert in https://github.com/supabase-community/supabase-kt/commit/31be2bd310ddc334aab14733fae0bfa76e24f2a4 

### Postgrest

* fix: add presence flag to join payload by @filipecabaco in https://github.com/supabase-community/supabase-kt/pull/1064
* quote and/or escape special characters in all postrest filters by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1066

### Docs / README

* Introduce new sample - Supa Sketcher - A realtime-white board application by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1067

### 3.2.4 - September 26, 2025

### Core

- Update Ktor to version `3.3.0`
- make kermit logger non-global by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1044

### Auth & Compose Auth

* Add support for linkIdentity with OIDC by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1051
  You can now link a Google / Apple account to an existing Supabase account via `linkIdentityWithIdToken`:
  ```kotlin
  supabase.auth.linkIdentityWithIdToken(provider = Google, idToken = "idToken received from native auth") {
      // optional stuff
  }
  ```
  To accomplish this with Compose Auth, set the `onIdToken` parameter in the `rememberSignInWith(...)` method:
  ```kotlin
  //default parameter is ComposeAuth.SIGN_IN_CALLBACK
  val state = composeAuth.rememberSignInWithGoogle(onIdToken = ComposeAuth.LINK_IDENTITY_CALLBACK)
  ```
  This also means, you can upgrade anonymous users via native auth and/or handle the id token received from native auth yourself (e.g. via your viewmodel)

### Samples

* Fix Chat sample launch fails on iOS by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/1050

### 3.2.3 - September 11, 2025

### Core

- Update Kotlin to version `2.2.20` (**due to `kotlinx.browser` changes, using this or a higher version might be required**)
- Remove atomicfu, migrate Atomic lists/maps, coroutine improvements by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1029
  Removed atomicfu dependency completely, improvements to atomic references / lists
- Fix min loglevel by @sproctor in https://github.com/supabase-community/supabase-kt/pull/951
- Re-add serializers into the bom dependency by @jan-tennert in https://github.com/supabase-community/supabase-kt-plugins/commit/5e34081ac1855b22ea3cd52a07fe9820a6c7a6cd

### Postgrest

- make filter values comply with postrest rules by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1036
  You can now use reserved characters in requests
- Add overloads for Postgrest insert, upsert and update requests by @manriif in https://github.com/supabase-community/supabase-kt/pull/1034
  You can now provide an already-serialized `JsonObject`/`JsonElement` to the `insert`/`upsert`/`update` methods

### Auth

- Unify Wasm JS and JS implementations by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1043

### Realtime

- Split callbacks in the CallbackManager by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1042
  This **might** increase performance drastically for applications using a lot of (different) callbacks

### 3.2.2 - July 19, 2025

### Core

- Update to Ktor `3.2.2`
- Fix [Sketch integration](https://github.com/supabase-community/supabase-kt-plugins/tree/main/SketchIntegration) not correctly publishing

### Postgrest

- improve postgrest error messages by @sproctor in https://github.com/supabase-community/supabase-kt/pull/1006

### Auth

- Fix IMPLICIT OAuth flow on Desktop targets by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/1005

### 3.2.1 - July 14, 2025

### Core

- Update KotlinX Datetime to version `0.7.1` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/995
- Update Ktor to version `3.2.1`
- Move plugins to its [own](https://github.com/supabase-community/supabase-kt-plugins) repository by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/955
  - Added a Sketch integration
  - In the future the versioning for the plugins will be separate from the main modules

### Functions

-Improve docs to put more context of the parameter when a function is renamed by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/993

### Realtime

-Add presence enabled flag by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/996

### 3.2.0 - June 25, 2025

### All modules

- Update to Kotlin `2.2.0`
- Update to Ktor `3.2.0`
- Don't swallow `CancellationException` by @sproctor in #895
- Only catch serialization exceptions in `bodyOrNull` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/904
- Fix simple warnings by @sproctor in https://github.com/supabase-community/supabase-kt/pull/915
- Use Dispatchers.IO by default on multi-threaded platforms by @sproctor in https://github.com/supabase-community/supabase-kt/pull/905
  - Configurable default dispatcher `SupabaseClientBuilder#coroutineDispatcher`, defaulting to `Dispatchers.IO` on supported targets.
  - Deprecated `AuthConfig#coroutineDispatcher`, replaced by the new client wide dispatcher.

### Core

* Add new standard headers by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/927

### Auth

* Add new event system, add support for error code in OTP links by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/912
  ```kotlin
  supabase.auth.events.collect { 
      when(it) {
          is AuthEvent.OtpError -> {
              println(it.errorCode)
          }
          is AuthEvent.RefreshFailure -> TODO()
      }
  }
  ```
  New event system which exists next to the `sessionStatus`, but works independently. Currently there are two events:
  - `AuthEvent.OtpError(...)` - will be emitted if an error code was found in a OTP link (deeplink on Android/iOS, URL on JS and Desktop)
  - `AuthEvent.RefreshFailure(cause)` - will be emitted if a session refresh failed (regardless if the session is still valid)

  This PR also changes the `SessionStatus.RefreshFailure(cause)`:
  - The `cause` parameter/property is deprecated (use the event for the cause)
  - This status will only get set, if the session expired.

  --> If a refresh failed, an event will always be emitted, but the session status will only get updated if the session also expired. Planning to rename the status in the future to something like `SessionStatus.NeedsRefresh`

  Additional changes:
  - Error related parameters will now be removed from the history when used
* (JS, WASM JS) Add an option to disable automatic url checking by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/96
  ```kotlin
  install(Auth) {
      disableUrlChecking = true //Default: false
  }
  ```
* Add support for providing a custom url launcher by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/933
  You can now provide a custom `UrlLauncher` for opening URLs in the OAuth flow:
  ```kotlin
  install(Auth)  {
      urlLauncher = UrlLauncher { supabase, url ->
          println("Opening URL: $url")
      }
  }
  ```
* Deprecate NativeSignInResult.NetworkError as it isn't used by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/952
* Deprecate minimalSettings in favor of minimalConfig by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/916
* Fix null result when sign up with Email provider by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/922
- Fix OAuth server listener resuming coroutine twice by @jan-tennert in #893
  A Desktop app using OAuth should no longer produce an exception when exiting the app after using `signInWith(OAuthProvider)`
- Add missing error codes by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/909:
  `EmailAddressInvalid`, `Web3ProviderDisabled`, `Web3UnsupportedChain`
- (JS/Wasm JS) Fix exception on non-session hash parameters and only consume used parameters by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/911
  Previously, all URL parameters / hash parameter were removed after successfully authenticating, this is no longer the case.
- Fix session expiration check by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/913
  The session will now also be refreshed when the current date time is under a certain threshold (20% before the actual expiration), instead of only after session expiration

### Postgrest

* Fix serialization exception occurring when `PostgrestRestException#details` is not a String by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/956

### 3.1.4 - April 1, 2025

### All modules

- Remove `sealed` modifier from interfaces where they are not needed (and prevent mocking in KMP) by @jan-tennert #883

### Core

- Fix custom serializer modules not being used in the `KotlinXSerializer` by @jan-tennert in #881

### 3.1.3 - March 17, 2025

### Auth

- Fix JS/Wasm hash/code check and removal by @Sparky983 and @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/875
Hashes & PKCE codes should now be correctly checked for on WASM, and removed from the browser history after usage.

### 3.1.2 - March 3, 2025

This version requires Ktor version `3.1.1` or higher.

### Core

- Migrate to new crypto dependency by @jan-tennert in https://github.com/supabase-community/supabase-kt/commit/7440814403c1a07a32740aeec0aa527a1c2a70c5
- Clean up internal `SupabaseClient` code by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/855
- Migrate to dokka v2 by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/856
- Update to Ktor version `3.1.1`

### Compose Auth

- Fix Native Apple Sign In on iOS by @yannickpulver in https://github.com/supabase-community/supabase-kt/pull/866

### 3.1.1 - February 5, 2025

### Core

* Make `HttpRequestException` a subclass of `IOException` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/847

### Realtime

* Catch any exceptions when sending a message in realtime by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/848

### 3.1.0 - January 31, 2025

### Auth

* Fix OAuth linking on JS/Wasm JS by @jan-tennert in https://github.com/supabase-community/supabase-kt/commit/d7dd01ab15995a360be91b28857a055eeb1e989d

### Realtime

* Improve behavior for realtime channel creation and improve docs by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/831
  If a channel with the same `channelId` exists, `Realtime#channel()` will return it instead of creating a new one. The channel will now also be saved after calling `Realtime#channel()` instead at subscribing.
* Add pull token approach to realtime by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/807
* Prevent sending expired tokens to realtime by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/808

### PostgREST

* Add custom exception for PostgREST API errors and include `HttpResponse` in `RestException`s by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/789
  - All PostgREST rest exceptions are a `PostgrestRestException`, which contain PostgREST fields like `hint`, `code`.
  - All `RestException`s now contain the full `HttpResponse` as a property.

### Storage

* Add support for file metadata, `info` and `exists` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/694

### Docs

* Update `supabaseModule.kt` references by @emmanuel-ferdman in https://github.com/supabase-community/supabase-kt/pull/823

### Compose Auth

* Make the dialog type for the Native Google Sign In configurable by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/832
  New `type` parameter for `rememberSignInWithGoogle()`, only functional on Android.

### 3.0.3 - December 4, 2024

### Core

- Add support for Kotlin `2.1.0`
- Fix build errors on certain targets like `wasm-js`

### Compose Auth & Auth

- Use `okio` for hashing instead of `krypto`
  Let me know if there are any issues!

### 3.0.2 - November 11, 2024

### Core

- Add support for Ktor `3.0.1` by @jan-tennert in #780

### Auth

- Add IDToken support for the `Kakao` `OAuthProvider` by @jan-tennert in #776
- Add missing `AuthErrorCode`s: `SessionExpired`, `RefreshTokenNotFound`, `RefreshTokenAlreadyUsed` by @jan-tennert in #775

### Compose Auth & Compose Auth UI

- Add support for Compose `1.7.0` by @jan-tennert in #759

### Realtime

- Remove additional `toMap()` call for `Realtime#subscriptions` to prevent rare exceptions by @jan-tennert in #779

### Coil3 Integration

- Add support for Coil3 version `3.0.2` by @jan-tennert in #780

### 3.0.1 - October 10, 2024

### Core

- Add support for Kotlin `2.0.21`
- Add support for Ktor `3.0.0`

### Auth

- Add HTTP Callback Server support for `mingwx64` (untested)

### Coil3 Integration

- Add support for Coil3 version `3.0.0-rc01`

### 3.0.0 - October 1, 2024

# Ktor 3

Starting with `3.0.0`, supabase-kt now uses Ktor 3. This brings WASM support, but projects using Ktor 2 will be incompatible.
Ktor  `3.0.0-rc-1` is used in this release.

# Changes

### Rename gotrue-kt to auth-kt and rename the package name

- **The `gotrue-kt` module is no longer being published starting with version `3.0.0`. Use the new `auth-kt` module.**
- **Rename `auth-kt` package name from `io.github.jan.supabase.gotrue` to `io.github.jan.supabase.auth`**.

### Support for WASM-JS

New `wasm-js` target for **supabase-kt**, **auth-kt**, **storage-kt**, **functions-kt**, **postgrest-kt**, **realtime-kt**, **compose-auth**, **compose-auth-ui**, **apollo-graphql** and the new **coil3-integration** by @jan-tennert in #311

### New plugin: coil3-integration

Support for Coil3 and all Compose Multiplatform targets under a new plugin by @jan-tennert in #428. [Checkout the documentation](https://github.com/supabase-community/supabase-kt/tree/master/plugins/Coil3Integration).
**The "old" coil 2 integration is still available and hasn't changed.**

### Auth

- Remove `Auth#modifyUser()`
- Remove `MfaApi#loggedInUsingMfa`, `MfaApi#loggedInUsingMfaFlow`, `MfaApi#isMfaEnabled`, `MfaApi#isMfaEnabledFlow`
- Refactor SessionStatus by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/725
  - Move `SessionStatus` to its own `status` package
  - Rename `SessionStatus#LoadingFromStorage` to `SessionStatus#Initializing`
  - Rename and refactor `SessionStatus#NetworkError` to `SessionStatus#RefreshFailure(cause)`
    *Note: The cause can be either `RefreshFailureCause#NetworkError` or `RefreshFailureCause#InternalServerError`. In both cases the refreshing will be retried and the session not cleared from storage. During that time, the session is obviously not usable.*

### Apollo GraphQL

- Migrate to Apollo GraphQL 4.0.0 by @jan-tennert in #692

### Storage

**Rework the uploading & downloading methods by @jan-tennert in #729**
- Each uploading method (upload, update, uploadAsFlow ...) now has a `options` DSL. Currently, you can configure three things:
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

### Postgrest

- Move all optional function parameters for `PostgrestQueryBuilder#select()`, `insert()`, `upsert()` and `Postgrest#rpc()` to the request DSL by @jan-tennert in #716
  Example:
```kotlin
supabase.from("table").upsert(myValue) {
    defaultToNull = false
    ignoreDuplicates = false
}
```
- Move the non-parameter variant of `Postgrest#rpc()` to the `Postgrest` interface. It was an extension function before by @jan-tennert in #726
- Add a non-generic parameter variant of `Postgrest#rpc()` to the `Postgrest` interface. This function will be called from the existing generic variant by @jan-tennert in #726
- Add a `schema` property to the `Postgrest#rpc` DSL by @jan-tennert in #716
- Fix `insert` and `upsert` requests failing when providing an empty `JsonObject` by @jan-tennert in #742

### Realtime

- Refactor internal event system for maintainability and readability by @jan-tennert #696
- `RealtimeChannel#presenceChangeFlow` is now a member function of `RealtimeChannel`. (It was an extension function before) by @jan-tennert in #697
- Move the implementation for `RealtimeChannel#broadcastFlow` and `RealtimeChannel#postgresChangeFlow` to a member function of `RealtimeChannel`. (Doesn't change anything in the public API) by @jan-tennert in #697
- Make the setter of `PostgresChangeFilter` private

## 2.6.0 - August 16, 2024

### Core

- Update Kotlin to `2.0.10`

### Postgrest

- Expose `headers` and `params` in `PostgrestRequestBuilder` by @jan-tennert in #689

You can now set custom headers & url parameters while making a postgrest request

### Auth

- **Add support for third-party auth by @jan-tennert in #688**
  
  You can now use third-party auth providers like Firebase Auth instead of Supabase Auth by specifying a `AccessTokenProvider` in the `SupabaseClientBuilder`:
    ```kotlin
    val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
        accessToken = {
              //fetch the third party token
             "my-token"
        }
    }
    ```
  This will be used for the `Authorization` header and other modules like Realtime and Storage integrations!
  **Note:** The `Auth` plugin cannot be used in combination and will throw an exception if used when setting `accessToken`.
- **Changes to Multi-Factor-Authentication by @jan-tennert in #681**
  - Refactor the syntax for enrolling MFA factors and add support for the Phone factor type:
    ```kotlin
    //Enrolling a phone factor
    val factor = client.auth.mfa.enroll(FactorType.Phone, friendlyName) {
        phone = "+123456789"
    }
    
    //Enrolling a TOTP factor
    val factor = client.auth.mfa.enroll(FactorType.TOTP, friendlyName) {
        issuer = "Issuer"
    }
    ```
  - Add a `channel` parameter to `MfaApi#createChallenge` to allow sending phone MFA messages to either `SMS` or `WHATSAPP`.
  - Deprecate `MfaApi#loggedInUsingMfa` and `MfaApi#isMfaEnabled` & their flow variants in favor of `MfaApi#status` and `MfaApi#statusFlow`:
    ```kotlin
    val (enabled, active) = client.auth.mfa.status
    
    //Flow variant
    client.auth.mfa.statusFlow.collect { (enabled, active) ->
        processChange(enabled, active)
    }
    ```
- Add `SlackOIDC` `OAuthProvider` by @jan-tennert in #688

### Realtime

- Remove client-side rate-limiting in #678 by @jan-tennert 
- Fix broadcasting to a private channel via the HTTP API in #673 by @jan-tennert 
- Fix callbacks not getting removed correctly in the `CallbackManager` in #673 by @jan-tennert 
- Change internal realtime implementation to be more robust and testable in #673 by @jan-tennert 
  - Add `Realtime.Config#websocketFactory`: This is highly internal and should be only modified if you know what you are doing

### Storage

- The `StorageApi#authenticatedRequest` method is now suspending
- All uploading methods will now return a `FileUploadResponse` instead of a `String`, which includes the actual path and some other properties.

## 2.5.4 - July 27, 2024

### Realtime
- Fix deletion in `postgresListDataFlow` not working correctly by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/671

## 2.5.3 - July 21, 2024

### Auth

- Add support for verifying a token hash by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/657

### Miscellaneous
- Add link to a RESTful service sample built with supabase-kt by @hieuwu in https://github.com/supabase-community/supabase-kt/pull/664

## 2.5.2 - July 08, 2024

### Realtime
- fix(realtime): add private to Realtime by @grdsdev in https://github.com/supabase-community/supabase-kt/pull/642
Add support for specifying if a channel is private via `RealtimeChannelBuilder#isPrivate`

### Compose Auth
- Fix Google sign in prompt in composeAuth by @Aaron246 in https://github.com/supabase-community/supabase-kt/pull/648
- fix(auth): fix sign in with Apple in ComposeAuth by @grdsdev in https://github.com/supabase-community/supabase-kt/pull/654

## 2.5.1 - June 28, 2024

### Auth

- Ignore certain status codes when signing out a user by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/633
  Failure of signing out because of invalid/expired JWTs will now be ignored and the local session cleared.

## 2.5.0 - June 9, 2024

### Auth
* Add error code enum for Auth API errors by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/618
  All rest errors containing a `error_code` field will now throw a `AuthRestException` rather than generic `BadRequestExceptions`, etc.
  `AuthRestException`s contain a `errorCode` field of the type `AuthErrorCode` containing all known error codes.
  Two error codes have their own exceptions (but obviously inherit from `AuthRestException)`: `AuthWeakPasswordException` and `AuthSessionMissingException`.
  API errors not containing this field will throw the generic exceptions.

* Handle `weak_password` and `session_not_found` auth error codes by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/596
  There is now a new subclass of `RestException`: `AuthRestExcepton` which will be a super class for exceptions based on error codes. Currently, there are two new exceptions: `AuthWeakPasswordException` and `AuthSessionMissingException`
* Fix/improve session expiry calculation by @JOsacky in #610

### Postgrest

* Add columns header to `PostgrestQueryBuilder#insert` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/611
  This fixes an issue when inserting a list of objects where some objects might not have all keys.
* Add `defaultToNull` parameter to `PostgrestQueryBuilder#insert`

### Realtime

* Use `Postgrest`s `propertyConversionMethod` for getting the property name of primary keys
* Add the possibility to have a multi-column PK in realtime by @iruizmar in https://github.com/supabase-community/supabase-kt/pull/614
* Fix presences updates for `RealtimeChannel#presenceDataFlow` working incorrectly by @JOsacky in https://github.com/supabase-community/supabase-kt/pull/607

### Compose Auth Ui

* Add `isError` parameter to Compose Auth Ui fields by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/612
* Add AuthUiExperimental annotation by @iruizmar in https://github.com/supabase-community/supabase-kt/pull/603
  All composables now have a `AuthUiExperimental` annotation instead of `SupabaseExperimental`

### Misc

* Update README.md by @rafalgawlik in https://github.com/supabase-community/supabase-kt/pull/605

## 2.4.3 - June 3, 2024

### Auth

- Fix a major bug causing sessions to be refreshed later than they should be in https://github.com/supabase-community/supabase-kt/commit/af9143d6f32c9c97c38870e8bdd20863edcbc38e by @jan-tennert
  _The `expiresAt` property was saved incorrectly, delaying the expiry date._

## 2.4.2 - May 19, 2024

### Auth

- Remove OtpType deprecation notice and clarify documentation by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/595
  The `SIGNUP` and `MAGIC_LINK` `OtpType`s are no longer deprecated as they are used for resending OTPs.
- Store session before emitting Authenticated state by @iruizmar in https://github.com/supabase-community/supabase-kt/pull/600
  This fixes a bug where it's possible to cancel any sign-in method and the `sessionStatus` gets set to `Authenticated` without the session actually saving to storage due to the cancellation.

## 2.4.1 - May 11, 2024

### Compose Auth UI

* Fix "Login in with" typo by @JOsacky in https://github.com/supabase-community/supabase-kt/pull/588

### Postgrest
* Include columns in upsert request & change default parameter value for `defaultToNull` to `true` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/590
  This fixes an issue when upserting a list of objects where some objects might not have all keys.

## 2.4.0 - May 9, 2024

### Core

- Update Kotlin to **2.0.0-RC2** and Compose to **1.6.10-rc01** by @jan-tennert in #581
- Rethrow `CancellationException`s in network requests by @jan-tennert in #578
- Log the actual error if a network request fails by @jan-tennert in https://github.com/supabase-community/supabase-kt/commit/0c93053e9ddb192f7238d719617d96eb71702bfd

### Auth

- Auth changes & fixes by @jan-tennert in #568
    - The method `Auth#linkIdentity` will now return the OAuth URL if the config value `ExternalAuthConfigDefaults.automaticallyOpenUrl` is set to false.
      Otherwise, null.
    - Fix the `autoRefresh` default value for `Auth#importSession` not being set to `config.alwaysAutoRefresh`
    - Add `codeVerifier` parameter to `MemoryCodeVerifierCache` to be able to set an initial value
    - Add missing `captchaToken` config option in the `OTP` auth provider
- Change the default session key for the `SettingsSessionManager` to work with multiple instances on the same device and add a `key` parameter to the constructor in case you want a custom key by @MohamedRejeb in #572

### Realtime

- Add new experimental extension functions to retrieve initial data and listen for updates without using realtime channels by @jan-tennert #579:
```kotlin
//Not a suspending function, subscribing and unsubscribing is handled internally
val myProductFlow: Flow<Product> = supabase.from("products").selectSingleValueAsFlow(Product::id) {
    Product::id eq 2
}.collect {
    println(it)
}
```
```kotlin
val productsFlow: Flow<List<Product>> = supabase.from("products").selectAsFlow(Product::id, filter = FilterOperation("id", FilterOperator.GT, 2)).collect {
    println(it)
}
```
This requires both `Realtime` and `Postgrest` to be installed within the SupabaseClient.

### Storage

- Prohibit uploading empty data to a bucket by @hieuwu in #577

### Functions

- Add new `region` parameter to invoke functions which allows changing the region where the Edge Function will be invoked in. Defaults to `Functions.Config#defaultRegion` (which is `FunctionRegion.ANY`) by @jan-tennert in #580 

## 2.3.1 - April 20, 2024

### Storage

- Make the default value for `Storage.Config.resumable#cache` `null` to prevent a NullPointerException in testing enviroments. (If it's set to null, the default cache implementation will be used)
- Fix `BucketApi#createSignedUploadUrl` returning an invalid url
- Fix `BucketApi#createSignedUrl` not correctly adding transformation parameters
- Fix `BucketApi#createSignedUrl` returning an invalid url
- Fix `BucketListFilter#sortBy` not working correctly
- Add missing `destinationBucket` parameter to `BucketApi#copy` and `BucketApi#move` by @jan-tennert in #562

### Auth

- Move `enableLifecycleCallbacks` to the common source set (still only used on Android) by @jan-tennert in #561
- Add `AuthConfig#minimalSettings` (useful for servers or testing, disabling session storage and uses only in-memory caches) by @jan-tennert in #561

### Realtime

- Remove the suspend modifier for `RealtimeChannel#postgresListDataFlow` by @jan-tennert in #563
- When a postgres flow is closed, the joining payload will now be correctly cleaned by @jan-tennert in #563

## 2.3.0 - April 15, 2024

### Core

- Update Kotlin to `2.0.0-RC1`

### Postgrest

- Add referenced table parameter to `or` and `and` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/551
- Fix nested `or` blocks working incorrectly
- Auto remove line breaks and white spaces when unquoted on Columns.Raw by @iruizmar in https://github.com/supabase-community/supabase-kt/pull/544
- Add support for using GET when calling database functions by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/538
  There is now a `method` parameter which takes in a `RpcMethod`. This can either be `POST`, `HEAD` or `GET`. The head parameter has been removed.

### Realtime

- Stabilize new flow presence and postgres methods by @jan-tennert in #535

### Auth

- Stabilize linking identities and signing in anonymously by @jan-tennert in #535
- Rename `Auth#modifyUser` to `Auth#updateUser` by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/534
- Return full a full `UserInfo` on sign up by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/528
  The `signUpWith` method will now return a `UserInfo` object for the `Email`, `Phone` and `IDToken` instead of separate Result objects.
- Allow customizing the custom tab intent on Android by @jan-tennert in https://github.com/supabase-community/supabase-kt/pull/529


