package com.pwfauth.demo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The site's palette (assets/css/site.css): black and white throughout, mint only for "active / ok". */
object Pwf {
    val bg = Color(0xFF0A0A0A)
    val surface = Color(0xFF121212)
    val surface2 = Color(0xFF181818)
    val line = Color(0xFF1F1F1F)
    val line2 = Color(0xFF2B2B2B)
    val text = Color(0xFFEDEDED)
    val mute = Color(0xFF8C8C8C)
    val mute2 = Color(0xFF636363)
    val mint = Color(0xFF3CCF91)
    val red = Color(0xFFF05252)
    val amber = Color(0xFFF5A524)
    val mono = FontFamily.Monospace
}

@Composable
fun PwfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White, onPrimary = Pwf.bg,
            background = Pwf.bg, onBackground = Pwf.text,
            surface = Pwf.surface, onSurface = Pwf.text,
            surfaceVariant = Pwf.surface2, onSurfaceVariant = Pwf.mute,
            outline = Pwf.line2, error = Pwf.red,
        ),
        typography = Typography(
            headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
        ),
        content = content,
    )
}

/** The PWF Auth mark: a white disc with the keyhole cut out (the site's #mark symbol, 32×32). */
@Composable
fun PwfMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 32f, pivot = Offset.Zero) {
            drawCircle(Color.White, radius = 16f, center = Offset(16f, 16f))
            val hole = Path().apply {
                moveTo(16f, 7.5f)
                // a 4.2 circle around (16, 11.7) that opens into the slot at the bottom
                arcTo(androidx.compose.ui.geometry.Rect(11.8f, 7.5f, 20.2f, 15.9f), -90f, -150f, false)
                lineTo(12.5f, 24f)
                lineTo(19.5f, 24f)
                lineTo(18f, 15.4f)
                arcTo(androidx.compose.ui.geometry.Rect(11.8f, 7.5f, 20.2f, 15.9f), 60f, -150f, false)
                close()
            }
            drawPath(hole, Pwf.bg)
        }
    }
}
