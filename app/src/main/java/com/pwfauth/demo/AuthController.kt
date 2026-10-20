package com.pwfauth.demo

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** What the home screen shows about the signed-in key or account. */
data class SessionInfo(
    val byAccount: Boolean,
    val who: String,            // the key, or the username
    val keyType: String,
    val duration: Int,          // with keyType: 30 + "days"
    val activatedAt: String,
    val expiresAt: String,      // "" = never
    val daysRemaining: Int?,
    val appName: String,
    val appMessage: String,
    val sessionId: String,
    val signedInAt: Long,
)

/** Why the session ended, from the heartbeat's kill switch (or the user's own sign-out: null). */
data class Ended(val code: String, val message: String)

/**
 * The app's one piece of state: signed out / signed in, and the live heartbeat.
 * Network calls run on Dispatchers.IO; Compose reads the fields below.
 */
class AuthController(context: Context) {
    private val client = PWFLicense(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var beatThread: Thread? = null

    val hwid: String = client.getHWID()

    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var session by mutableStateOf<SessionInfo?>(null); private set
    var lastBeat by mutableStateOf(0L); private set
    var beats by mutableStateOf(0); private set
    var ended by mutableStateOf<Ended?>(null); private set

    fun loginKey(key: String) = signIn(false) { client.login(key.trim()) }

    fun loginAccount(user: String, pass: String) = signIn(true) { client.loginAccount(user.trim(), pass) }

    private fun signIn(byAccount: Boolean, call: () -> JSONObject) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val r = try {
                withContext(Dispatchers.IO) { call() }
            } catch (e: Exception) {
                JSONObject().put("success", false).put("message", e.message ?: "Cannot reach the license server.")
            }
            busy = false
            if (!r.optBoolean("success", false)) {
                error = r.optString("message", "").ifEmpty { "Sign-in was refused." }
                return@launch
            }
            val u = r.optJSONObject("user") ?: JSONObject()
            val app = r.optJSONObject("app") ?: JSONObject()
            session = SessionInfo(
                byAccount = byAccount,
                who = if (byAccount) u.optString("username") else u.optString("license_key"),
                keyType = u.optString("key_type"),
                duration = u.optInt("duration", 0),
                activatedAt = u.optString("activated_at"),
                expiresAt = u.optString("expires_at").takeUnless { it == "null" }.orEmpty(),
                daysRemaining = if (u.isNull("days_remaining")) null else u.optInt("days_remaining"),
                appName = app.optString("name"),
                appMessage = app.optString("message"),
                sessionId = r.optString("session_id"),
                signedInAt = System.currentTimeMillis(),
            )
            lastBeat = System.currentTimeMillis()
            beats = 0
            startHeartbeat()
        }
    }

    // the kill switch: a ban, pause, expiry, HWID reset or ended session in the panel reaches the phone here
    private fun startHeartbeat() {
        beatThread?.interrupt()
        beatThread = Thread({
            client.runHeartbeat(
                onBeat = { scope.launch { lastBeat = System.currentTimeMillis(); beats++ } },
                onRevoked = { code, message -> scope.launch { finish(Ended(code, message)) } },
            )
        }, "pwfauth-heartbeat").apply { isDaemon = true; start() }
    }

    fun logout() {
        beatThread?.interrupt()
        scope.launch {
            try { withContext(Dispatchers.IO) { client.logout() } } catch (_: Exception) { /* signed out here anyway */ }
            finish(null)
        }
    }

    private fun finish(why: Ended?) {
        beatThread = null
        session = null
        ended = why
    }

    fun dismissEnded() { ended = null }
    fun clearError() { error = null }
}
