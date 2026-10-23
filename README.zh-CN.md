<p align="center"><a href="README.md">English</a> · <b>简体中文</b> · <a href="README.tr.md">Türkçe</a> · <a href="README.ar.md">العربية</a></p>
<p align="center">视频教程：<a href="https://www.youtube.com/@pwfauth">youtube.com/@pwfauth</a></p>

# PWF Auth Android 示例

一个受 [PWF Auth](https://pwfauth.com) 保护的小型 Android 应用，演示许可证密钥、客户账户和远程停用（kill switch），使用 Kotlin 和 Jetpack Compose 编写

- **登录**：使用许可证密钥或客户账户（用户名和密码）登录
- **一个密钥对应一部手机**：每部手机都有自己的设备 ID，由 Android ID 和你的应用包名生成
- **实时会话**：应用每 30 秒向服务器报到一次，后台的 Sessions 页面会显示这部手机
- **远程停用**：在后台封禁、暂停某个密钥或让它过期，下一次报到时应用就会关闭，并告诉用户原因
- **加密并签名**：每个请求和响应都用你的应用密钥（app secret）加密和签名（AES-256 与 HMAC-SHA256）

## 运行

1. 在 [pwfauth.com](https://pwfauth.com) 的后台创建一个应用（Applications → New application），然后在 Licences 页面生成几个密钥
2. 克隆本仓库

   ```bash
   git clone https://github.com/pwfauth/pwfauth-android
   ```

3. 将 `local.properties.example` 复制为 `local.properties`，并填入你的应用密钥（Applications → 你的应用 → App secret）

   ```properties
   PWF_APP_SECRET=your-app-secret
   ```

4. 用 Android Studio 打开该文件夹并运行，或在终端中执行

   ```bash
   ./gradlew installDebug
   ```

`local.properties` 已列入 `.gitignore`，所以你的密钥不会被提交到 Git。不过和所有客户端一样，它仍会被打包进 APK：请在 release 构建中开启代码压缩（R8），`app/build.gradle.kts` 已经这样配置

## 在你自己的应用中使用

1. 在后台打开 **SDK generator**，选择你的应用和 **Android** 标签页，下载 `PWFLicense.kt`
2. 把它放进你的 app 模块，例如 `app/src/main/java/com/pwfauth/sdk/PWFLicense.kt`
3. 在 `AndroidManifest.xml` 中添加网络权限

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. 在主线程之外登录，然后启动心跳

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

客户账户的用法相同：调用 `client.loginAccount(username, password)`，然后启动同样的心跳

需要 Android 8.0（minSdk 26）或更高版本，Gradle 中无需额外依赖：`org.json` 和 `HttpURLConnection` 都是 Android 自带的

## 目录说明

| 路径 | 说明 |
| --- | --- |
| `app/src/main/java/com/pwfauth/demo/PWFLicense.kt` | 客户端：与 SDK generator 为 Android 生成的代码相同，只是从 `BuildConfig` 读取密钥 |
| `app/src/main/java/com/pwfauth/demo/AuthController.kt` | 登录、心跳线程和远程停用，以 Compose 状态的形式提供 |
| `app/src/main/java/com/pwfauth/demo/ui/` | 登录界面、实时会话界面，以及说明会话结束原因的窗口 |

## 许可

MIT，详见 [LICENSE](LICENSE)
