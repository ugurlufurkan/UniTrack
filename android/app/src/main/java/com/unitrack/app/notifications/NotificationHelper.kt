package com.unitrack.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unitrack.app.MainActivity
import com.unitrack.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Etkinlik/ders hatırlatma bildirimlerinin tek gösterim noktası.
 *
 * Kanal, uygulama ilk açıldığında (bkz. UniTrackApp ya da ilk worker
 * tetiklendiğinde) bir kere oluşturulur — `createNotificationChannel`
 * idempotent, tekrar tekrar çağrılması güvenlidir.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "unitrack_event_reminders"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Takvim Hatırlatmaları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Ders, sınav, ödev ve diğer takvim etkinlikleri için hatırlatmalar."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * [notificationId] iş parçacığı/olay bazında benzersiz olmalı — aynı ID
     * ikinci kez gösterilirse önceki bildirimin yerini alır (StatusBar'da
     * çakışan hatırlatmalar birikmesin diye event+offset'e göre türetiliyor,
     * bkz. EventNotificationScheduler.notificationIdFor).
     */
    fun show(notificationId: Int, title: String, body: String) {
        ensureChannel()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS reddedilmiş/verilmemişse (Android 13+) sistem
        // zaten göstermeyecektir; SecurityException'a karşı savunmacı çağrı.
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
