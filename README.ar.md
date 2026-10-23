<p align="center"><a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.tr.md">Türkçe</a> · <b>العربية</b></p>
<p align="center">شروحات بالفيديو: <a href="https://www.youtube.com/@pwfauth">youtube.com/@pwfauth</a></p>

# مثال PWF Auth لأندرويد

تطبيق أندرويد صغير محمي بـ [PWF Auth](https://pwfauth.com): مفاتيح الترخيص وحسابات العملاء والإيقاف عن بُعد، مكتوب بلغة Kotlin وواجهة Jetpack Compose

- **تسجيل الدخول** بمفتاح ترخيص أو بحساب عميل (اسم المستخدم وكلمة المرور)
- **مفتاح واحد لهاتف واحد**: لكل هاتف معرّف جهاز خاص به، يُبنى من معرّف Android واسم حزمة تطبيقك
- **جلسة مباشرة**: يتواصل التطبيق مع الخادم كل 30 ثانية، ويظهر الهاتف في صفحة Sessions في لوحة التحكم
- **الإيقاف عن بُعد**: احظر مفتاحاً أو أوقفه مؤقتاً أو أنهِ صلاحيته من لوحة التحكم، وعند التحقق التالي يُغلَق التطبيق ويُخبَر المستخدم بالسبب
- **مشفّر وموقّع**: كل طلب وكل رد محميّان بسر التطبيق (AES-256 و HMAC-SHA256)

## التشغيل

1. أنشئ تطبيقاً في لوحة التحكم على [pwfauth.com](https://pwfauth.com) (Applications → New application)، ثم أنشئ بعض المفاتيح من صفحة Licences
2. استنسخ هذا المستودع

   ```bash
   git clone https://github.com/pwfauth/pwfauth-android
   ```

3. انسخ `local.properties.example` إلى `local.properties` وضع فيه سر التطبيق (من صفحة Applications، ثم تطبيقك، ثم App secret)

   ```properties
   PWF_APP_SECRET=your-app-secret
   ```

4. افتح المجلد في Android Studio وشغّله، أو شغّله من سطر الأوامر

   ```bash
   ./gradlew installDebug
   ```

الملف `local.properties` مُدرج في `.gitignore`، لذلك لن يصل سرّك إلى Git أبداً. لكنه يبقى داخل ملف APK كما في أي تطبيق عميل: فعّل تقليص الشيفرة (R8) في نسخة الإصدار (release)، كما يفعل `app/build.gradle.kts` أصلاً

## الاستخدام في تطبيقك

1. افتح صفحة **SDK generator** في لوحة التحكم، واختر تطبيقك ثم تبويب **Android**، ونزّل الملف `PWFLicense.kt`
2. ضع الملف في وحدة التطبيق (app module)، مثلاً `app/src/main/java/com/pwfauth/sdk/PWFLicense.kt`
3. أضف صلاحية الإنترنت إلى `AndroidManifest.xml`

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. سجّل الدخول خارج مؤشر الترابط الرئيسي، ثم شغّل التحقق الدوري (heartbeat)

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

تعمل حسابات العملاء بالطريقة نفسها: `client.loginAccount(username, password)`، ثم التحقق الدوري نفسه

يعمل المثال على Android 8.0 (minSdk 26) أو أحدث، دون أي اعتماديات إضافية في Gradle، فكلٌّ من `org.json` و `HttpURLConnection` جزء من Android

## محتويات المستودع

| المسار | الوصف |
| --- | --- |
| `app/src/main/java/com/pwfauth/demo/PWFLicense.kt` | العميل: الشيفرة نفسها التي يولّدها SDK generator لأندرويد، مع قراءة السر من `BuildConfig` |
| `app/src/main/java/com/pwfauth/demo/AuthController.kt` | تسجيل الدخول ومؤشر ترابط التحقق الدوري والإيقاف عن بُعد، على شكل حالة Compose |
| `app/src/main/java/com/pwfauth/demo/ui/` | شاشة تسجيل الدخول وشاشة الجلسة المباشرة والنافذة التي توضّح سبب انتهاء الجلسة |

## الرخصة

مرخّص بموجب MIT، راجع [LICENSE](LICENSE)
