package id.my.ionlinestudio.jadwalsholatindonesia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.WebSecondary

@Composable
fun PrayerIcon(
    prayerKey: String,
    modifier: Modifier = Modifier,
    tint: Color = WebSecondary
) {
    when (prayerKey.lowercase()) {
        "subuh", "fajr" -> Icon(
            imageVector = Icons.Default.WbTwilight,
            contentDescription = "Subuh",
            modifier = modifier,
            tint = tint
        )
        "syuruq", "sunrise" -> Icon(
            imageVector = Icons.Default.WbTwilight,
            contentDescription = "Syuruq",
            modifier = modifier,
            tint = tint
        )
        "dzuhur", "dhuhr" -> Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "Dzuhur",
            modifier = modifier,
            tint = tint
        )
        "ashar", "asr" -> Icon(
            imageVector = Icons.Default.WbCloudy,
            contentDescription = "Ashar",
            modifier = modifier,
            tint = tint
        )
        "maghrib" -> Icon(
            imageVector = Icons.Default.WbTwilight,
            contentDescription = "Maghrib",
            modifier = modifier,
            tint = tint
        )
        "isya", "isha" -> Icon(
            imageVector = Icons.Default.Brightness3,
            contentDescription = "Isya",
            modifier = modifier,
            tint = tint
        )
        else -> Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "Prayer",
            modifier = modifier,
            tint = tint
        )
    }
}
