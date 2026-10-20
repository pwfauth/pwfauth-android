// PWF Auth SDK v2.1 · Kotlin for Android
// The generator's Kotlin client, made to run on a phone:
//   - HttpURLConnection instead of java.net.http (Android has no java.net.http)
//   - the device id comes from Android (ANDROID_ID), not /etc/machine-id (apps may not read it)
//   - org.json is part of Android: nothing to add in Gradle
// Requires minSdk 26 (java.util.Base64) and <uses-permission android:name="android.permission.INTERNET"/>.
// Every call blocks: call it from a background thread or Dispatchers.IO, never from the UI thread.
package com.pwfauth.demo

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

// AES-256-CBC envelope encryption — mirrors the server's PayloadCrypto.
class CryptoEnvelope(appSecret: String) {
    private val encKey = sha256("enc:" + appSecret)
    private val macKey = sha256("mac:" + appSecret)

    fun encrypt(data: JSONObject): String {
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(iv))
        val ct = cipher.doFinal(data.toString().toByteArray(Charsets.UTF_8))
        val p = Base64.getEncoder().encodeToString(iv + ct)
        val t = System.currentTimeMillis() / 1000L
        return JSONObject().put("p", p).put("t", t).put("s", hmacHex(p + t)).toString()
    }

    fun decrypt(envelopeJson: String): JSONObject {
        val env = JSONObject(envelopeJson)
        if (!env.has("p") || !env.has("t") || !env.has("s")) throw IllegalStateException("Invalid envelope format")
        val p = env.getString("p")
        val t = env.getLong("t")
        val s = env.getString("s").lowercase()
        // constant-time compare, so the HMAC can't be guessed byte by byte
        if (!MessageDigest.isEqual(hmacHex(p + t).toByteArray(), s.toByteArray()))
            throw SecurityException("HMAC verification failed")
        if (Math.abs(System.currentTimeMillis() / 1000L - t) > MAX_DRIFT)
            throw SecurityException("Request expired (replay protection) - check the phone's date and time")
        val combined = Base64.getDecoder().decode(p)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(combined, 0, 16))
        return JSONObject(String(cipher.doFinal(combined, 16, combined.size - 16), Charsets.UTF_8))
    }

    private fun hmacHex(msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(macKey, "HmacSHA256"))
        return mac.doFinal(msg.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_DRIFT = 300L // keep in sync with the server
        internal fun sha256(s: String): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
    }
}

/**
 * [hwid] is this phone's id for the license server. On a phone use the Context constructor:
 * `PWFLicense(context)`. The String one is for tests and for apps that bring their own id.
 */
class PWFLicense(private val hwid: String, private var baseUrl: String = BASE_URL, private val appSecret: String = APP_SECRET) {
    constructor(context: Context) : this(androidHwid(context))

    private val crypto = CryptoEnvelope(appSecret)
    @Volatile var sessionId: String? = null
        private set
    var licenseKey: String? = null
        private set
    private var heartbeatInterval = 30 // seconds; overwritten from the login response

    fun getHWID(): String = hwid

    // Shared reply handler for every transport. Surfaces HTTP/transport failures
    // with a readable message instead of a cryptic JSON exception, then
    // transparently decrypts the body when the server enveloped it.
    private fun parseReply(raw: String?, status: Int): JSONObject {
        if (raw.isNullOrBlank()) throw IOException("License server returned HTTP $status with an empty body.")
        val probe = try {
            JSONObject(raw)
        } catch (e: Exception) {
            // Not JSON at all — usually a proxy/CDN error page or a wrong base URL.
            throw IOException("License server returned HTTP $status with a non-JSON body. Check the API base URL.")
        }
        if (probe.has("p") && probe.has("t") && probe.has("s")) return crypto.decrypt(raw)
        if (status >= 400 && !probe.has("success")) throw IOException("License server returned HTTP $status.")
        return probe // plain server response (its own success/error_code shape)
    }

    private class Reply(val status: Int, val body: String?)

    private fun send(method: String, endpoint: String, body: String?, headers: Map<String, String>): Reply {
        val c = URL(baseUrl + endpoint).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 15_000
            c.useCaches = false
            c.setRequestProperty("X-App-Secret", appSecret)
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) {
                c.doOutput = true
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = c.responseCode
            val stream = if (status >= 400) c.errorStream else c.inputStream
            return Reply(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    // Send once more when the connection drops before any reply: a connection that sat idle
    // can be closed on the way ("Connection reset"). A reply, even an error reply, is never
    // sent twice, and neither is a timeout.
    private fun sendWithRetry(method: String, endpoint: String, body: String?, headers: Map<String, String>): Reply {
        try {
            return send(method, endpoint, body, headers)
        } catch (e: SocketTimeoutException) {
            throw e
        } catch (e: IOException) {
            Thread.sleep(300)
        }
        try {
            return send(method, endpoint, body, headers)
        } catch (e: SocketTimeoutException) {
            throw e
        } catch (e: IOException) {
            // Still no reply: the name may be blocked on this network. Same server, second name.
            if (baseUrl != BASE_URL || BACKUP_URL.isEmpty()) throw e
            baseUrl = BACKUP_URL
            return send(method, endpoint, body, headers)
        }
    }

    // Encrypt the request, send it, and hand the reply to parseReply.
    private fun post(endpoint: String, body: JSONObject): JSONObject {
        val r = sendWithRetry("POST", endpoint, crypto.encrypt(body), mapOf("Content-Type" to "application/json"))
        return parseReply(r.body, r.status)
    }

    // login.php also activates the key on first use — no separate activate call.
    fun login(licenseKey: String): JSONObject {
        val result = post("/api/auth/login.php", JSONObject()
            .put("license_key", licenseKey)
            .put("hwid", getHWID()))
        if (result.optBoolean("success", false)) {
            sessionId = result.optString("session_id", null)
            this.licenseKey = licenseKey
            heartbeatInterval = result.optInt("heartbeat_interval", 30)
        }
        return result
    }

    // Lightweight status + feature-map check WITHOUT opening a session
    // (no device seat is consumed) — handy before the user logs in.
    fun checkKey(licenseKey: String): JSONObject =
        post("/api/auth/check-key.php", JSONObject().put("license_key", licenseKey))

    fun heartbeat(): JSONObject? {
        val sid = sessionId ?: return null
        return post("/api/auth/heartbeat.php", JSONObject()
            .put("session_id", sid)
            .put("license_key", licenseKey))
    }

    // Blocking loop (run it on a background thread). Pings on the server interval and
    // REACTS to the kill switch: a ban / pause / expire / HWID-reset / revoke makes the
    // next heartbeat return success=false, and we call onRevoked(code, message) then stop.
    // onBeat (optional) hears every answered beat, e.g. to show "last check-in" in the app.
    //
    // A single network blip is retried, but MAX_HEARTBEAT_FAILURES consecutive failures
    // also end the session with NETWORK_LOST — the server drops the session anyway, so
    // firewalling this domain must not leave a working app.
    fun runHeartbeat(onBeat: ((JSONObject) -> Unit)? = null, onRevoked: (code: String, message: String) -> Unit) {
        val kill = setOf("BANNED", "PAUSED", "EXPIRED", "HWID_RESET", "MAINTENANCE",
            "SESSION_REVOKED", "SESSION_EXPIRED", "SESSION_MISMATCH")
        var failures = 0
        while (sessionId != null) {
            try {
                Thread.sleep(heartbeatInterval * 1000L)
            } catch (e: InterruptedException) {
                return
            }
            val r = try { heartbeat() } catch (e: Exception) { null }
            if (r == null) {
                if (sessionId == null) return                // logged out meanwhile
                if (++failures >= MAX_HEARTBEAT_FAILURES) {
                    sessionId = null
                    onRevoked("NETWORK_LOST", "Cannot reach the license server. Please check your connection and sign in again.")
                    return
                }
                continue                                     // transient — retry next tick
            }
            failures = 0
            if (r.optBoolean("success", false)) {
                onBeat?.invoke(r)
                continue
            }
            val code = r.optString("error_code", "")
            if (code in kill) {
                sessionId = null                             // session is dead server-side
                onRevoked(code, r.optString("message", ""))
                return
            }
            // Unknown non-success: treat as transient and keep trying.
        }
    }

    // GET whose RESPONSE is enveloped. Pass the license key for Bearer endpoints.
    private fun get(endpoint: String, bearerKey: String?): JSONObject {
        val h = if (bearerKey.isNullOrEmpty()) emptyMap() else mapOf("Authorization" to "Bearer $bearerKey")
        val r = sendWithRetry("GET", endpoint, null, h)
        return parseReply(r.body, r.status)
    }

    // POST plain JSON — endpoints that do NOT speak the envelope.
    private fun postPlain(endpoint: String, body: JSONObject): JSONObject {
        val r = sendWithRetry("POST", endpoint, body.toString(), mapOf("Content-Type" to "application/json"))
        return parseReply(r.body, r.status)
    }

    // App metadata + social links + server "what's new" (name, version, download URL, maintenance flags).
    fun getAppInfo(): JSONObject = get("/api/app/info.php", null)

    // Remote texts for THIS license (per-key overrides win over app defaults). Key rides in Authorization: Bearer.
    fun getTexts(): JSONObject = get("/api/app/text.php", licenseKey)

    // Mint a free trial key for this device. Off unless the owner enabled trials (else TRIAL_DISABLED / TRIAL_USED / TRIAL_LIMIT).
    fun createTrial(): JSONObject = postPlain("/api/auth/trial.php", JSONObject().put("hwid", getHWID()))

    // Queue an HWID-reset request for admin review (the reply is deliberately identical whether or not the key exists).
    fun requestHwidReset(reason: String): JSONObject =
        postPlain("/api/auth/request-hwid-reset.php", JSONObject().put("license_key", licenseKey).put("reason", reason))

    // Create an end-user account. Only works while the owner has user accounts switched on for this app.
    fun registerAccount(username: String, password: String, email: String): JSONObject =
        postPlain("/api/auth/account-register.php", JSONObject().put("username", username).put("password", password).put("email", email))

    // Sign an account in and open a session bound to this phone. Start the heartbeat afterwards, exactly as after a key login — the kill switch works the same way.
    fun loginAccount(username: String, password: String): JSONObject {
        val result = postPlain("/api/auth/account-login.php", JSONObject().put("username", username).put("password", password).put("hwid", getHWID()))
        if (result.optBoolean("success", false)) {
            sessionId = result.optString("session_id", null)
            licenseKey = if (result.has("license_key")) result.optString("license_key", null) else null
            heartbeatInterval = result.optInt("heartbeat_interval", 30)
        }
        return result
    }

    fun logout(): JSONObject? {
        val sid = sessionId ?: return null
        sessionId = null
        return post("/api/auth/logout.php", JSONObject()
            .put("session_id", sid)
            .put("license_key", licenseKey))
    }

    companion object {
        private const val MAX_HEARTBEAT_FAILURES = 3
        // The generated file has your app's values here; this project reads them from local.properties (BuildConfig).
        private val BASE_URL = BuildConfig.PWF_BASE_URL
        private val APP_SECRET = BuildConfig.PWF_APP_SECRET
        // The same license server under a second name, used when the first gives no reply at all
        // (some networks block it: Etisalat in the UAE). Only for the live server.
        private val BACKUP_URL = if (BuildConfig.PWF_BASE_URL == "https://pwfauth.com") "https://pwfauth.net" else ""

        /**
         * This phone's id: ANDROID_ID, hashed with the app's package so the raw id never leaves the phone.
         * ANDROID_ID stays the same for this app on this phone (also after a reinstall, signed with the same key);
         * a factory reset makes a new one. Without it (very rare) a random id kept in the app's storage.
         */
        @SuppressLint("HardwareIds")
        fun androidHwid(context: Context): String {
            val app = context.applicationContext
            var id = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
            if (id.isNullOrBlank() || id == "9774d56d682e549c") {   // the id some old emulators share
                val prefs = app.getSharedPreferences("pwfauth", Context.MODE_PRIVATE)
                id = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
                    prefs.edit().putString("device_id", it).apply()
                }
            }
            val hash = CryptoEnvelope.sha256("pwfauth:" + id + ":" + app.packageName).joinToString("") { "%02x".format(it) }
            return "android-" + hash.take(32)
        }
    }
}

// Usage (in an Activity or ViewModel):
//   val client = PWFLicense(context)
//   val result = withContext(Dispatchers.IO) { client.login("YOUR-LICENSE-KEY") }
//   if (result.optBoolean("success")) {
//       // Keep the session alive AND obey the admin kill switch (loops until revoked):
//       Thread {
//           client.runHeartbeat { code, message ->
//               // code in {BANNED, PAUSED, EXPIRED, HWID_RESET, MAINTENANCE,
//               //          SESSION_REVOKED, SESSION_EXPIRED, SESSION_MISMATCH, NETWORK_LOST}
//               // back to the sign-in screen (switch to the main thread first)
//           }
//       }.start()
//   }
