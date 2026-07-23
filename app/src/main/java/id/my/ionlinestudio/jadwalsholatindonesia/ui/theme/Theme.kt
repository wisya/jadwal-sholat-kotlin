package id.my.ionlinestudio.jadwalsholatindonesia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WebDarkColorScheme = darkColorScheme(
    primary = WebPrimary,
    onPrimary = WebOnPrimary,
    primaryContainer = WebPrimaryContainer,
    onPrimaryContainer = WebOnPrimaryContainer,
    secondary = WebSecondary,
    onSecondary = WebOnSecondary,
    background = WebBackground,
    surface = WebSurface,
    surfaceVariant = WebSurfaceVariant,
    onBackground = WebTextPrimary,
    onSurface = WebTextPrimary,
    onSurfaceVariant = WebTextSecondary
)

@Composable
fun JadwalSholatIndonesiaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WebDarkColorScheme,
        typography = Typography,
        content = content
    )
}