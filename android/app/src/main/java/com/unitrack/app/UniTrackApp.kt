package com.unitrack.app // Kendi paket adınla aynı olduğundan emin ol

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class UniTrackApp : Application(), Configuration.Provider {

    // WorkManager'ın, @HiltWorker ile işaretlenmiş worker'ları (bkz.
    // EventReminderWorker) Hilt bağımlılıklarıyla (repository, notification
    // helper) üretebilmesi için gereken köprü.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        // super.onCreate() burada Hilt'in üretilen kodunu tetikliyor ve
        // `workerFactory` alanını inject ediyor. WorkManager'ı ancak bundan
        // SONRA başlatabiliriz — AndroidManifest.xml'de varsayılan
        // WorkManagerInitializer bilinçli olarak kaldırıldı, çünkü o,
        // Application.onCreate()'ten önce (workerFactory henüz inject
        // edilmeden) çalışıp çöküyordu.
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
    }
}
