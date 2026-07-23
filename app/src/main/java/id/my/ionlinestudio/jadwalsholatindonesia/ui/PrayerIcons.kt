package id.my.ionlinestudio.jadwalsholatindonesia.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import id.my.ionlinestudio.jadwalsholatindonesia.R
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.WebSecondary

@Composable
fun PrayerIcon(
    prayerKey: String,
    modifier: Modifier = Modifier,
    tint: Color = WebSecondary
) {
    val drawableRes = when (prayerKey.lowercase()) {
        "subuh", "fajr" -> R.drawable.ic_prayer_subuh
        "syuruq", "sunrise" -> R.drawable.ic_prayer_syuruq
        "dzuhur", "dhuhr" -> R.drawable.ic_prayer_dzuhur
        "ashar", "asr" -> R.drawable.ic_prayer_ashar
        "maghrib" -> R.drawable.ic_prayer_maghrib
        "isya", "isha" -> R.drawable.ic_prayer_isya
        else -> R.drawable.ic_prayer_dzuhur
    }

    Icon(
        painter = painterResource(id = drawableRes),
        contentDescription = prayerKey,
        modifier = modifier,
        tint = tint
    )
}
