package ir.hesabyar.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0B1F3A)
private val Blue = Color(0xFF2563EB)
private val Sky = Color(0xFFDBEAFE)
private val Gold = Color(0xFFF4B740)
private val Paper = Color(0xFFF5F7FB)
private val Ink = Color(0xFF132238)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Sky,
    onPrimaryContainer = Navy,
    secondary = Gold,
    onSecondary = Navy,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9EEF6),
    onSurfaceVariant = Color(0xFF44546A),
    error = Color(0xFFB42318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Navy,
    primaryContainer = Color(0xFF173C72),
    secondary = Gold,
    background = Color(0xFF071426),
    surface = Color(0xFF10233D),
    onSurface = Color(0xFFF3F6FB)
)

@Composable
fun HesabYarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
