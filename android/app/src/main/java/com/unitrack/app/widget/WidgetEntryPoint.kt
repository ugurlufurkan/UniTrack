package com.unitrack.app.widget

import com.unitrack.app.data.repository.AcademicRepository
import com.unitrack.app.data.repository.AuthPreferences
import com.unitrack.app.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

/**
 * GlanceAppWidget, Compose tarafındaki gibi @HiltViewModel/hiltViewModel() ile
 * inject edilemez (Activity/Fragment yaşam döngüsüne bağlı değil). Bu yüzden
 * ihtiyaç duyduğu repository'lere bu EntryPoint üzerinden, applicationContext'ten
 * elle erişiyoruz — projede zaten @Singleton olan aynı örnekler kullanılır,
 * ayrı bir Retrofit/Repository örneği oluşturulmaz.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun academicRepository(): AcademicRepository
    fun taskRepository(): TaskRepository
    fun authPreferences(): AuthPreferences
}

fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
