package id.my.ionlinestudio.jadwalsholatindonesia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

private val WebLightColorScheme = lightColorScheme(
    primary = WebLightPrimary,
    onPrimary = WebLightOnPrimary,
    primaryContainer = WebLightPrimaryContainer,
    onPrimaryContainer = WebLightOnPrimaryContainer,
    secondary = WebLightSecondary,
    onSecondary = WebLightOnSecondary,
    background = WebLightBackground,
    surface = WebLightSurface,
    surfaceVariant = WebLightSurfaceVariant,
    onBackground = WebLightTextPrimary,
    onSurface = WebLightTextPrimary,
    onSurfaceVariant = WebLightTextSecondary
)

@Composable
fun JadwalSholatIndonesiaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WebDarkColorScheme else WebLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}