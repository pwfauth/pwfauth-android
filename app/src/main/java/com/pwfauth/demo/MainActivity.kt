package com.pwfauth.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.remember
import com.pwfauth.demo.ui.EndedDialog
import com.pwfauth.demo.ui.HomeScreen
import com.pwfauth.demo.ui.PwfTheme
import com.pwfauth.demo.ui.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PwfTheme {
                val auth = remember { (application as? DemoApp)?.auth ?: AuthController(applicationContext) }
                AnimatedContent(auth.session, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
                    if (s == null) SignInScreen(auth) else HomeScreen(auth, s)
                }
                auth.ended?.let { EndedDialog(it) { auth.dismissEnded() } }
            }
        }
    }
}
