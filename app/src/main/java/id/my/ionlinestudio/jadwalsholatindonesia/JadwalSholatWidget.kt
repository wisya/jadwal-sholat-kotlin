package id.my.ionlinestudio.jadwalsholatindonesia

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JadwalSholatWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.jadwal_sholat_widget)

    // Calculate prayer times directly (Offline)
    // For widget, we'll use a fixed location (e.g. Jakarta/Yogyakarta) or 0 elevation
    // to ensure instantaneous update without network delays on the home screen.
    val lat = -7.8014
    val lng = 110.3644
    val coordinates = Coordinates(lat, lng)
    
    val params = CalculationMethod.OTHER.parameters
    params.fajrAngle = 20.0
    params.ishaAngle = 18.0
    params.madhab = Madhab.SHAFI
    params.adjustments.fajr = 2
    params.adjustments.sunrise = -2 
    params.adjustments.dhuhr = 2
    params.adjustments.asr = 2
    params.adjustments.maghrib = 2 
    params.adjustments.isha = 2

    val date = DateComponents.from(Date())
    val prayerTimes = PrayerTimes(coordinates, date, params)
    
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    views.setTextViewText(R.id.tv_subuh, formatter.format(prayerTimes.fajr))
    views.setTextViewText(R.id.tv_dzuhur, formatter.format(prayerTimes.dhuhr))
    views.setTextViewText(R.id.tv_ashar, formatter.format(prayerTimes.asr))
    views.setTextViewText(R.id.tv_maghrib, formatter.format(prayerTimes.maghrib))
    views.setTextViewText(R.id.tv_isya, formatter.format(prayerTimes.isha))

    appWidgetManager.updateAppWidget(appWidgetId, views)
}