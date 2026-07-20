package com.unitrack.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Tek bir etkinlik hatırlatmasını (bir event + bir bildirim ofseti) tetikleyen
 * worker. Zamanlama tarafı [EventNotificationScheduler]'da — bu worker sadece
 * "artık zamanı geldi, bildirimi göster" işini yapar.
 */
@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: ""

        notificationHelper.show(notificationId, title, body)
        return Result.success()
    }

    companion object {
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
