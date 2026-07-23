package id.my.ionlinestudio.jadwalsholatindonesia

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import id.my.ionlinestudio.jadwalsholatindonesia.data.LocationPreferences
import id.my.ionlinestudio.jadwalsholatindonesia.data.PrayerCalculator
import java.text.SimpleDateFormat
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

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, JadwalSholatWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.jadwal_sholat_widget)

    val locationPrefs = LocationPreferences(context)
    val userLocation = locationPrefs.getLocation()

    val prayerTimes = PrayerCalculator.calculatePrayerTimes(
        latitude = userLocation.latitude,
        longitude = userLocation.longitude,
        elevation = userLocation.elevation
    )

    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    views.setTextViewText(R.id.tv_subuh, formatter.format(prayerTimes.fajr))
    views.setTextViewText(R.id.tv_dzuhur, formatter.format(prayerTimes.dhuhr))
    views.setTextViewText(R.id.tv_ashar, formatter.format(prayerTimes.asr))
    views.setTextViewText(R.id.tv_maghrib, formatter.format(prayerTimes.maghrib))
    views.setTextViewText(R.id.tv_isya, formatter.format(prayerTimes.isha))

    appWidgetManager.updateAppWidget(appWidgetId, views)
}