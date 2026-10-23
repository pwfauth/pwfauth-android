<p align="center"><b>English</b> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.tr.md">Türkçe</a> · <a href="README.ar.md">العربية</a></p>
<p align="center">Video tutorials: <a href="https://www.youtube.com/@pwfauth">youtube.com/@pwfauth</a></p>

# PWF Auth for Android

A small Android app protected by [PWF Auth](https://pwfauth.com): licence keys, customer accounts and the kill switch, in Kotlin and Jetpack Compose

- **Sign in** with a licence key or a customer account (username and password)
- **One key, one phone**: each phone has its own device ID, made from Android's ID and your app's package
- **Live session**: the app checks in every 30 seconds, and the panel shows the phone in Sessions
- **Kill switch**: ban, pause or expire a key in the panel, and the next check-in closes the app and tells the user why
- **Encrypted and signed**: every request and reply is sealed with your app secret (AES-256 and HMAC-SHA256)

## Run it

1. Create an application in your panel at [pwfauth.com](https://pwfauth.com) (Applications → New application), then generate a few keys in Licences
2. Clone this repository

   ```bash
   git clone https://github.com/pwfauth/pwfauth-android
   ```

3. Copy `local.properties.example` to `local.properties` and put your app secret in it (Applications → your app → App secret)

   ```properties
   PWF_APP_SECRET=your-app-secret
   ```

4. Open the folder in Android Studio and run it, or from a terminal

   ```bash
   ./gradlew installDebug
   ```

`local.properties` is in `.gitignore`, so your secret never lands in Git. It still ends up inside the APK, as in every client: turn on code shrinking (R8) for your release build, as `app/build.gradle.kts` already does

## Use it in your own app

1. In your panel, open **SDK generator**, pick your app and the **Android** tab, and download `PWFLicense.kt`
2. Put it in your app module, for example `app/src/main/java/com/pwfauth/sdk/PWFLicense.kt`
3. Add the internet permission to `AndroidManifest.xml`

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. Sign in off the main thread, then start the heartbeat

   ```kotlin
   val client = PWFLicense(context)

   lifecycleScope.launch {
       val result = withContext(Dispatchers.IO) { client.login(key) }
       if (!result.optBoolean("success")) {
           showError(result.optString("message"))      // for example "This license key does not exist."
           return@launch
       }
       openApp()
       thread {
           client.runHeartbeat { code, message ->
               // BANNED, PAUSED, EXPIRED, HWID_RESET, SESSION_REVOKED, NETWORK_LOST …
               runOnUiThread { closeApp(code, message) }
           }
       }
   }
   ```

Customer accounts work the same way: `client.loginAccount(username, password)`, then the same heartbeat

It needs Android 8.0 (minSdk 26) or newer and nothing extra in Gradle: `org.json` and `HttpURLConnection` are part of Android

## What is in here

| Path | What it is |
| --- | --- |
| `app/src/main/java/com/pwfauth/demo/PWFLicense.kt` | The client: the same code the SDK generator writes for Android, reading the secret from `BuildConfig` |
| `app/src/main/java/com/pwfauth/demo/AuthController.kt` | Sign-in, the heartbeat thread and the kill switch, as Compose state |
| `app/src/main/java/com/pwfauth/demo/ui/` | The sign-in screen, the live session screen and the window that says why a session ended |

## License

MIT, see [LICENSE](LICENSE)
