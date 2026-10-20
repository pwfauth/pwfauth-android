package com.pwfauth.demo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwfauth.demo.AuthController
import com.pwfauth.demo.BuildConfig

@Composable
fun SignInScreen(auth: AuthController) {
    var byAccount by rememberSaveable { mutableStateOf(false) }
    var key by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val canGo = !auth.busy && if (byAccount) user.isNotBlank() && pass.isNotEmpty() else key.isNotBlank()
    val go = {
        if (canGo) {
            focus.clearFocus()
            if (byAccount) auth.loginAccount(user, pass) else auth.loginKey(key)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Pwf.bg).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        PwfMark(44.dp)
        Spacer(Modifier.height(28.dp))
        Text("Sign in", style = MaterialTheme.typography.headlineSmall, color = Pwf.text)
        Spacer(Modifier.height(6.dp))
        Text("This app is protected by PWF Auth. Use your license key or your account", style = MaterialTheme.typography.bodyMedium, color = Pwf.mute)
        Spacer(Modifier.height(28.dp))

        Segmented(byAccount) { byAccount = it; auth.clearError() }
        Spacer(Modifier.height(16.dp))

        AnimatedContent(byAccount, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "form") { acc ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!acc) {
                    Field(key, { key = it.uppercase() }, "License key", Icons.Outlined.Key, mono = true,
                        caps = KeyboardCapitalization.Characters, ime = ImeAction.Go, onIme = go)
                } else {
                    Field(user, { user = it }, "Username", Icons.Outlined.Person, ime = ImeAction.Next, onIme = { focus.moveFocus(FocusDirection.Down) })
                    Field(pass, { pass = it }, "Password", Icons.Outlined.Lock, password = true, ime = ImeAction.Go, onIme = go)
                }
            }
        }

        AnimatedVisibility(auth.error != null) {
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Pwf.red.copy(alpha = .08f)).border(1.dp, Pwf.red.copy(alpha = .3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(auth.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF9C9C))
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(if (byAccount) "Sign in" else "Activate", busy = auth.busy, enabled = canGo, onClick = go)

        Spacer(Modifier.height(64.dp))
        DeviceFooter(auth.hwid)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Segmented(byAccount: Boolean, onChange: (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Pwf.surface)
        .border(1.dp, Pwf.line, RoundedCornerShape(12.dp)).padding(4.dp)) {
        val half = maxWidth / 2
        val x by animateDpAsState(if (byAccount) half else 0.dp, label = "seg")
        Box(Modifier.offset(x = x).width(half).fillMaxSize().clip(RoundedCornerShape(9.dp)).background(Pwf.surface2)
            .border(1.dp, Pwf.line2, RoundedCornerShape(9.dp)))
        Row(Modifier.fillMaxSize()) {
            listOf(false to "License key", true to "Account").forEach { (v, label) ->
                Box(Modifier.weight(1f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { onChange(v) },
                    contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = if (byAccount == v) Pwf.text else Pwf.mute)
                }
            }
        }
    }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, hint: String, icon: ImageVector,
    password: Boolean = false, mono: Boolean = false, caps: KeyboardCapitalization = KeyboardCapitalization.None,
    ime: ImeAction = ImeAction.Done, onIme: () -> Unit = {},
) {
    var shown by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).background(Pwf.surface)
            .border(1.dp, if (focused) Pwf.mute2 else Pwf.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Pwf.mute, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Pwf.mute2, fontSize = 15.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = Pwf.text, fontSize = 15.sp, fontFamily = if (mono) Pwf.mono else null, letterSpacing = if (mono) 0.5.sp else 0.sp),
                cursorBrush = SolidColor(Pwf.text),
                visualTransformation = if (password && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(capitalization = caps, autoCorrectEnabled = false,
                    keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii, imeAction = ime),
                keyboardActions = KeyboardActions(onAny = { onIme() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (password) {
            Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (shown) "Hide password" else "Show password",
                tint = Pwf.mute, modifier = Modifier.size(20.dp).clickable { shown = !shown })
        }
    }
}

@Composable
fun PrimaryButton(label: String, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
            .background(if (enabled || busy) Color.White else Color.White.copy(alpha = .18f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Pwf.bg, strokeWidth = 2.dp)
        else Text(label, color = if (enabled) Pwf.bg else Pwf.mute, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeviceFooter(hwid: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("This device", style = MaterialTheme.typography.labelMedium, color = Pwf.mute2)
        Text(hwid, fontFamily = Pwf.mono, fontSize = 12.sp, color = Pwf.mute)
        Text(BuildConfig.PWF_BASE_URL.removePrefix("https://").removePrefix("http://"), fontSize = 12.sp, color = Pwf.mute2)
    }
}
