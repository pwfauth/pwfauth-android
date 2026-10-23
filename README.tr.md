<p align="center"><a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <b>Türkçe</b> · <a href="README.ar.md">العربية</a></p>
<p align="center">Video eğitimleri: <a href="https://www.youtube.com/@pwfauth">youtube.com/@pwfauth</a></p>

# Android için PWF Auth

[PWF Auth](https://pwfauth.com) ile korunan küçük bir Android uygulaması: lisans anahtarları, müşteri hesapları ve uzaktan durdurma (kill switch), Kotlin ve Jetpack Compose ile yazıldı

- **Giriş**: lisans anahtarıyla veya bir müşteri hesabıyla (kullanıcı adı ve şifre) giriş yapılır
- **Bir anahtar, bir telefon**: her telefonun, Android kimliğinden ve uygulamanızın paket adından üretilen kendi cihaz kimliği vardır
- **Canlı oturum**: uygulama her 30 saniyede bir sunucuya bağlanır ve panel telefonu Sessions sayfasında gösterir
- **Uzaktan durdurma**: panelde bir anahtarı yasaklayın, duraklatın veya süresini bitirin; bir sonraki kontrolde uygulama kapanır ve kullanıcıya nedenini bildirir
- **Şifreli ve imzalı**: her istek ve yanıt, uygulama gizli anahtarınızla (app secret) şifrelenir ve imzalanır (AES-256 ve HMAC-SHA256)

## Çalıştırma

1. [pwfauth.com](https://pwfauth.com) adresindeki panelinizde bir uygulama oluşturun (Applications → New application), ardından Licences sayfasında birkaç anahtar üretin
2. Bu depoyu klonlayın

   ```bash
   git clone https://github.com/pwfauth/pwfauth-android
   ```

3. `local.properties.example` dosyasını `local.properties` adıyla kopyalayın ve uygulama gizli anahtarınızı içine yazın (Applications → uygulamanız → App secret)

   ```properties
   PWF_APP_SECRET=your-app-secret
   ```

4. Klasörü Android Studio'da açıp çalıştırın veya terminalden şu komutu verin

   ```bash
   ./gradlew installDebug
   ```

`local.properties` dosyası `.gitignore` içinde yer aldığı için gizli anahtarınız hiçbir zaman Git'e girmez. Yine de her istemcide olduğu gibi APK'nın içinde bulunur: release derlemeniz için kod küçültmeyi (R8) açın, `app/build.gradle.kts` bunu zaten yapıyor

## Kendi uygulamanızda kullanma

1. Panelinizde **SDK generator** sayfasını açın, uygulamanızı ve **Android** sekmesini seçin, ardından `PWFLicense.kt` dosyasını indirin
2. Dosyayı uygulama modülünüze koyun, örneğin `app/src/main/java/com/pwfauth/sdk/PWFLicense.kt`
3. `AndroidManifest.xml` dosyasına internet iznini ekleyin

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. Girişi ana iş parçacığının dışında yapın, ardından düzenli kontrolü (heartbeat) başlatın

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

Müşteri hesapları da aynı şekilde çalışır: `client.loginAccount(username, password)` çağrısının ardından aynı heartbeat başlatılır

Android 8.0 (minSdk 26) veya üstü gerekir ve Gradle'a ek bir bağımlılık eklemeniz gerekmez: `org.json` ve `HttpURLConnection` zaten Android'in parçasıdır

## Bu depoda neler var

| Yol | Ne işe yarar |
| --- | --- |
| `app/src/main/java/com/pwfauth/demo/PWFLicense.kt` | İstemci: SDK generator'ın Android için ürettiği kodun aynısı, gizli anahtarı `BuildConfig` üzerinden okur |
| `app/src/main/java/com/pwfauth/demo/AuthController.kt` | Giriş, heartbeat iş parçacığı ve uzaktan durdurma, Compose state olarak |
| `app/src/main/java/com/pwfauth/demo/ui/` | Giriş ekranı, canlı oturum ekranı ve oturumun neden sona erdiğini gösteren pencere |

## Lisans

MIT, ayrıntılar için [LICENSE](LICENSE) dosyasına bakın
