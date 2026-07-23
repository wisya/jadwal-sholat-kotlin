package id.my.ionlinestudio.jadwalsholatindonesia

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Date
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val isReminder = intent.getBooleanExtra("IS_REMINDER", false)
        
        val title = if (isReminder) "Persiapan Sholat" else "Waktu Sholat"
        val message = if (isReminder) {
            "Waktu sholat $prayerName kurang 10 menit lagi. Segera bersiap!"
        } else {
            "Telah masuk waktu sholat $prayerName."
        }
        
        showNotification(context, title, message)
    }
    
    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "JADWAL_SHOLAT_CHANNEL"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jadwal Sholat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi adzan dan pengingat waktu sholat"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // Restore alarms after device reboot using default/saved location
            AlarmScheduler.scheduleAllPrayers(context)
        }
    }
}

object AlarmScheduler {
    fun scheduleAllPrayers(context: Context, lat: Double = -7.8014, lng: Double = 110.3644) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cek izin (khusus Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

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

        val prayerTimes = PrayerTimes(coordinates, DateComponents.from(Date()), params)
        
        val prayers = listOf(
            Pair("Subuh", prayerTimes.fajr),
            Pair("Dzuhur", prayerTimes.dhuhr),
            Pair("Ashar", prayerTimes.asr),
            Pair("Maghrib", prayerTimes.maghrib),
            Pair("Isya", prayerTimes.isha)
        )
        
        val now = Date()
        var requestCodeCounter = 100
        
        for ((name, time) in prayers) {
            // 1. Jadwalkan Adzan Tepat Waktu
            if (time.after(now)) {
                scheduleExactAlarm(context, alarmManager, time.time, name, false, requestCodeCounter++)
            }
            
            // 2. Jadwalkan Pengingat 10 Menit Sebelumnya
            val reminderTime = Calendar.getInstance().apply { 
                timeInMillis = time.time
                add(Calendar.MINUTE, -10)
            }.time
            
            if (reminderTime.after(now)) {
                scheduleExactAlarm(context, alarmManager, reminderTime.time, name, true, requestCodeCounter++)
            }
        }
    }
    
    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        timeInMillis: Long,
        prayerName: String,
        isReminder: Boolean,
        requestCode: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("IS_REMINDER", isReminder)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }
}
