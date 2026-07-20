package com.unitrack.app.notifications

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.data.dto.EventNotificationDto
import com.unitrack.app.ui.calendar.EventTypeUi
import com.unitrack.app.ui.calendar.formatNotificationOffset
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Etkinliklerin `notifications` (gün/saat/dakika önce) listesini gerçek,
 * zamanlanmış local bildirimlere çeviren tek nokta.
 *
 * CalendarViewModel her `refresh()` başarılı olduğunda (ilk yükleme, CRUD
 * sonrası, pull-to-refresh) [scheduleForEvents] çağırır — böylece kullanıcı
 * bir etkinlik ekleyip/düzenleyip/silip Takvim sekmesini her ziyaret
 * ettiğinde zamanlamalar güncel kalır.
 *
 * Her (event, bildirim-index) çifti WorkManager'da BENZERSİZ bir iş adıyla
 * (`event_reminder_<eventId>_<index>`) `ExistingWorkPolicy.REPLACE` ile
 * kuyruklanır: aynı etkinlik yeniden zamanlanınca eski iş otomatik yerini
 * yeni olana bırakır. Bir etkinliğin bildirim sayısı azalırsa (ör. 3 -> 1)
 * fazlalık kalan eski işler, önce o etkinliğin TAG'ine göre tamamı iptal
 * edilip sıfırdan kuyruklanarak temizlenir.
 */
@Singleton
class EventNotificationScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleForEvents(events: List<EventDto>) {
        events.forEach { scheduleForEvent(it) }
    }

    private fun scheduleForEvent(event: EventDto) {
        val tag = tagFor(event.id)

        // Önce bu etkinliğe ait TÜM eski zamanlamaları temizle — bildirim
        // sayısı azaldıysa veya bildirimler kapatıldıysa artık geçersiz
        // olan işler kuyrukta kalmasın.
        workManager.cancelAllWorkByTag(tag)

        if (!event.notificationsEnabled) return
        if (event.status == "completed" || event.status == "cancelled") return

        val startInstant = event.startAt.toInstantOrNull() ?: return
        val now = System.currentTimeMillis()

        event.notifications.forEachIndexed { index, notification ->
            val offsetMillis = notification.toOffsetMillis()
            val triggerMillis = startInstant.toEpochMilli() - offsetMillis
            val delay = triggerMillis - now

            // Geçmişte kalan (ör. uygulama uzun süre açılmadığı için tetiklenme
            // anı geçmiş) bildirimleri zamanlamıyoruz — kullanıcı uygulamayı
            // açtığında "geçmiş" bir bildirim patlamasın diye.
            if (delay <= 0) return@forEachIndexed

            val notificationId = notificationIdFor(event.id, index)
            val data = workDataOf(
                EventReminderWorker.KEY_NOTIFICATION_ID to notificationId,
                EventReminderWorker.KEY_TITLE to titleFor(event),
                EventReminderWorker.KEY_BODY to bodyFor(event, notification)
            )

            val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build()

            workManager.enqueueUniqueWork(
                uniqueWorkNameFor(event.id, index),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    /** Etkinlik silindiğinde/CRUD dışı bir yoldan kaldırıldığında kalan zamanlamaları temizler. */
    fun cancelForEvent(eventId: String) {
        workManager.cancelAllWorkByTag(tagFor(eventId))
    }

    private fun titleFor(event: EventDto): String {
        val typeLabel = EventTypeUi.fromApiValue(event.type).label
        return "$typeLabel: ${event.title}"
    }

    private fun bodyFor(event: EventDto, notification: EventNotificationDto): String {
        val offsetLabel = formatNotificationOffset(
            notification.daysBefore,
            notification.hoursBefore,
            notification.minutesBefore
        )
        val timeLabel = event.startAt.toInstantOrNull()
            ?.let { TIME_FORMATTER.format(it.atZoneSameInstant()) }
            ?: ""
        val locationSuffix = event.location?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
        return "$offsetLabel · $timeLabel$locationSuffix"
    }

    private fun EventNotificationDto.toOffsetMillis(): Long =
        TimeUnit.DAYS.toMillis(daysBefore.toLong()) +
            TimeUnit.HOURS.toMillis(hoursBefore.toLong()) +
            TimeUnit.MINUTES.toMillis(minutesBefore.toLong())

    private fun tagFor(eventId: String) = "event_reminder_$eventId"

    private fun uniqueWorkNameFor(eventId: String, index: Int) = "event_reminder_${eventId}_$index"

    /** WorkManager bildirim ID'si Int olmak zorunda — event UUID'sinden deterministik türetiliyor. */
    private fun notificationIdFor(eventId: String, index: Int): Int =
        (eventId.hashCode() * 31 + index) and 0x7fffffff

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("tr"))
    }
}

private fun String.toInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (e: DateTimeParseException) {
    null
}

private fun Instant.atZoneSameInstant() = this.atZone(java.time.ZoneId.systemDefault())
