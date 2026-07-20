package com.unitrack.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// AuthPreferences'taki "auth_prefs" ile aynı desen: DataStore delegate'i
// dosya seviyesinde (class içinde DEĞİL) tanımlanmalı, aksi halde "there are
// multiple DataStores active for this file" crash'i alınır.
//
// @PublishedApi internal: private olsaydı, aşağıdaki public inline/reified
// save()/load() fonksiyonları buna erişemezdi ("Public-API inline function
// cannot access non-public-API property"). internal + @PublishedApi, bunu
// modül dışına sızdırmadan inline fonksiyonların kullanmasına izin verir.
@PublishedApi
internal val Context.offlineCacheStore by preferencesDataStore(name = "offline_cache")

/**
 * Basit "stale-while-revalidate" tipi çevrimdışı önbellek.
 *
 * AcademicRepository, ağdan veri çekemediğinde (internet yok, timeout, DNS
 * hatası -> IOException) buraya düşer ve en son başarıyla çekilmiş veriyi
 * döner. Böylece ekranlar internetsizken asla "boş/hata" haline düşmez;
 * en azından kullanıcının en son gördüğü veri gösterilmeye devam eder.
 *
 * Sunucudan gelen gerçek hata cevapları (400/401/404 vb.) bu mekanizmayı
 * TETİKLEMEZ — onlar zaten bir cevap, sadece IOException (bağlantı hiç
 * kurulamadı) cache'e düşer. Bu ayrım bilinçli: sunucunun "bu işlem geçersiz"
 * demesiyle telefonun internete hiç ulaşamaması aynı şey değildir.
 */
@Singleton
class OfflineCache @Inject constructor(
    // Aynı sebepten: public inline fun'ların erişebilmesi için @PublishedApi internal.
    @ApplicationContext @PublishedApi internal val context: Context,
    @PublishedApi internal val json: Json
) {

    suspend inline fun <reified T> save(key: String, value: T) {
        val payload = json.encodeToString(value)
        context.offlineCacheStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = payload
        }
    }

    suspend inline fun <reified T> load(key: String): T? {
        val payload = context.offlineCacheStore.data.first()[stringPreferencesKey(key)]
            ?: return null

        return try {
            json.decodeFromString<T>(payload)
        } catch (e: Exception) {
            // Şema değiştiyse (ör. DTO'ya yeni zorunlu alan eklendi) eski
            // cache'i sessizce yok say; bir sonraki başarılı ağ isteği
            // zaten üzerine yazacak.
            null
        }
    }

    /** Çıkış yapıldığında bir sonraki kullanıcının verisiyle karışmasın diye çağrılır. */
    suspend fun clear() {
        context.offlineCacheStore.edit { it.clear() }
    }
}

/** AcademicRepository içindeki uç noktalar için sabit cache anahtarları. */
object CacheKeys {
    const val DASHBOARD = "dashboard"
    const val SEMESTERS = "semesters"
    const val COURSES = "courses"
    const val GPA = "gpa"
    const val STATISTICS = "statistics"
    const val TRANSCRIPT = "transcript"
    const val GRADE_SCALE = "grade_scale"
    const val CALENDAR_SUMMARY = "calendar_summary"
    const val CALENDAR_EVENTS_ALL = "calendar_events_all"
    const val CALENDAR_UPCOMING_EVENTS = "calendar_upcoming_events"
    const val CALENDAR_SCHEDULE = "calendar_schedule"
    const val TASKS_ALL = "tasks_all"
    const val ATTENDANCE_OVERVIEW = "attendance_overview"
    const val ATTENDANCE_COURSES = "attendance_courses"
    const val ATTENDANCE_COURSE_DETAIL_PREFIX = "attendance_course_detail_"
}
