package com.pwfauth.demo.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwfauth.demo.AuthController
import com.pwfauth.demo.Ended
import com.pwfauth.demo.SessionInfo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(auth: AuthController, s: SessionInfo) {
    // one clock for "x s ago" and the session length
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    Column(
        Modifier.fillMaxSize().background(Pwf.bg).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwfMark(30.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.appName.ifEmpty { "PWF Auth Demo" }, style = MaterialTheme.typography.titleMedium, color = Pwf.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Protected by PWF Auth", style = MaterialTheme.typography.bodySmall, color = Pwf.mute)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).border(1.dp, Pwf.line2, CircleShape).clickable { auth.logout() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.Logout, "Sign out", tint = Pwf.mute, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(if (s.byAccount) "Welcome, ${s.who}" else "You're in", style = MaterialTheme.typography.headlineSmall, color = Pwf.text)
        Spacer(Modifier.height(6.dp))
        Text("Everything below comes from the license server", style = MaterialTheme.typography.bodyMedium, color = Pwf.mute)
        if (s.appMessage.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(s.appMessage, style = MaterialTheme.typography.bodyMedium, color = Pwf.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Pwf.surface)
                    .border(1.dp, Pwf.line, RoundedCornerShape(12.dp)).padding(14.dp))
        }

        Spacer(Modifier.height(24.dp))
        LiveCard(lastBeatAgo = ((now - auth.lastBeat) / 1000).coerceAtLeast(0), beats = auth.beats, since = now - s.signedInAt, sessionId = s.sessionId)

        Spacer(Modifier.height(14.dp))
        Card {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (s.byAccount) "Account" else "License", style = MaterialTheme.typography.labelMedium, color = Pwf.mute2)
                    Spacer(Modifier.height(4.dp))
                    Text(s.who, fontFamily = if (s.byAccount) null else Pwf.mono, fontSize = 15.sp, color = Pwf.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusPill("Active", Pwf.mint)
            }
            Hairline()
            InfoRow("Plan", plan(s))
            Hairline()
            InfoRow("Activated", nice(s.activatedAt))
            Hairline()
            InfoRow("Expires", if (s.expiresAt.isEmpty()) "Never" else nice(s.expiresAt) + (s.daysRemaining?.let { " · $it days left" } ?: ""))
            Hairline()
            InfoRow("Device", auth.hwid, mono = true)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LiveCard(lastBeatAgo: Long, beats: Int, since: Long, sessionId: String) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val r by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "r")
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    drawCircle(Pwf.mint.copy(alpha = (1f - r) * .35f), radius = size.minDimension / 2 * (0.45f + r * 0.55f))
                    drawCircle(Pwf.mint, radius = size.minDimension / 2 * 0.36f)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Session live", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Pwf.text)
                Text("Last check-in ${lastBeatAgo}s ago · $beats heartbeats", style = MaterialTheme.typography.bodySmall, color = Pwf.mute)
            }
            Text(clock(since), fontFamily = Pwf.mono, fontSize = 14.sp, color = Pwf.text)
        }
        Hairline()
        InfoRow("Session", sessionId.take(8) + "…" + sessionId.takeLast(4), mono = true)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Pwf.surface).border(1.dp, Pwf.line, RoundedCornerShape(16.dp))) {
        content()
    }
}

@Composable
private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(Pwf.line))

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Pwf.mute, modifier = Modifier.width(92.dp))
        Text(value, fontSize = if (mono) 12.5.sp else 14.sp, fontFamily = if (mono) Pwf.mono else null, color = Pwf.text,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusPill(text: String, tone: Color) {
    Row(Modifier.clip(RoundedCornerShape(99.dp)).background(tone.copy(alpha = .12f)).border(1.dp, tone.copy(alpha = .3f), RoundedCornerShape(99.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tone))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tone)
    }
}

/** The panel's kill switch reached the phone: say what happened, then back to sign-in. */
@Composable
fun EndedDialog(e: Ended, onOk: () -> Unit) {
    val (icon, title) = when (e.code) {
        "BANNED" -> Icons.Outlined.Block to "This license was banned"
        "PAUSED" -> Icons.Outlined.PauseCircle to "This license is paused"
        "EXPIRED" -> Icons.Outlined.Schedule to "This license has expired"
        "HWID_RESET" -> Icons.Outlined.PhonelinkErase to "This device was reset"
        "NETWORK_LOST" -> Icons.Outlined.CloudOff to "Connection lost"
        "MAINTENANCE" -> Icons.Outlined.Schedule to "Under maintenance"
        else -> Icons.Outlined.Block to "Your session was ended"
    }
    val tone = if (e.code == "NETWORK_LOST" || e.code == "MAINTENANCE" || e.code == "PAUSED") Pwf.amber else Pwf.red
    AlertDialog(
        onDismissRequest = onOk,
        containerColor = Pwf.surface,
        icon = { Box(Modifier.size(48.dp).clip(CircleShape).background(tone.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tone) } },
        title = { Text(title, color = Pwf.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (e.message.isNotBlank()) Text(e.message, color = Pwf.mute, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text(e.code, fontFamily = Pwf.mono, fontSize = 12.sp, color = Pwf.mute2)
            }
        },
        confirmButton = { TextButton(onClick = onOk) { Text("OK", color = Pwf.text) } },
    )
}

private fun plan(s: SessionInfo): String = when {
    s.keyType == "lifetime" -> "Lifetime"
    s.keyType.isEmpty() -> "—"
    s.duration > 0 -> "${s.duration} " + (if (s.duration == 1) s.keyType.removeSuffix("s") else s.keyType)   // "30 days", "1 hour"
    else -> s.keyType.replaceFirstChar { it.uppercase() }
}

private val SHOW = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault())

/** The server's UTC stamps ("2026-10-20T17:54:18Z" or "2026-10-20 17:54:18") in the phone's own time. */
private fun nice(stamp: String): String {
    if (stamp.isBlank() || stamp == "null") return "—"
    return try {
        SHOW.format(Instant.parse(stamp.trim().replace(' ', 'T').let { if (it.endsWith("Z")) it else it + "Z" }))
    } catch (e: Exception) {
        stamp
    }
}

private fun clock(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    return if (t >= 3600) "%d:%02d:%02d".format(t / 3600, t / 60 % 60, t % 60) else "%02d:%02d".format(t / 60, t % 60)
}
