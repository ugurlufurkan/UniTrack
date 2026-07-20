package com.unitrack.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// Diğer DataStore'larla aynı desen: delegate dosya seviyesinde tanımlı.
private val Context.examPeriodStore by preferencesDataStore(name = "exam_period_prefs")

/**
 * Kullanıcının kendi belirlediği "sınav haftası" tarih aralığı.
 *
 * Madde #25 (Weather Effect — sınav haftası ambient tonu) için: backend'de
 * "final tarihi" diye bir alan/tablo YOK (Stage 4'teki Takvim özelliğiyle
 * birlikte gelecek). Sahte/uydurma bir tarih kullanmak yerine — ki bu,
 * kullanıcıya var olmayan bir veriyle yanlış bir "canlılık" hissi verirdi —
 * kullanıcının GoalPreferences'taki hedef GANO'ya verdiği gibi, kendi
 * sınav haftasını elle girebileceği dürüst, gerçekten çalışan bir alan.
 *
 * Cihazda saklanıyor (backend'e senkronize edilmiyor) — GoalPreferences'la
 * aynı gerekçeyle: bu bir iş kuralı değil, tamamen kişisel/geçici bir
 * hatırlatma ayarı.
 */
@Singleton
class ExamPeriodPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val EXAM_START_EPOCH_DAY = longPreferencesKey("exam_start_epoch_day")
        private val EXAM_END_EPOCH_DAY = longPreferencesKey("exam_end_epoch_day")
    }

    /** null: kullanıcı hiç sınav haftası tanımlamamış. */
    val examPeriod: Flow<ExamPeriod?> = context.examPeriodStore.data.map { prefs ->
        val startEpochDay = prefs[EXAM_START_EPOCH_DAY]
        val endEpochDay = prefs[EXAM_END_EPOCH_DAY]

        if (startEpochDay == null || endEpochDay == null) {
            null
        } else {
            ExamPeriod(
                start = LocalDate.ofEpochDay(startEpochDay),
                end = LocalDate.ofEpochDay(endEpochDay)
            )
        }
    }

    suspend fun setExamPeriod(start: LocalDate, end: LocalDate) {
        // Kullanıcı tarihleri ters seçerse (bitiş < başlangıç) sessizce
        // takas ediyoruz — hatalı bir aralık kaydetmektense düzeltmek
        // daha kullanıcı dostu.
        val (from, to) = if (start.isAfter(end)) end to start else start to end

        context.examPeriodStore.edit { prefs ->
            prefs[EXAM_START_EPOCH_DAY] = from.toEpochDay()
            prefs[EXAM_END_EPOCH_DAY] = to.toEpochDay()
        }
    }

    suspend fun clearExamPeriod() {
        context.examPeriodStore.edit { prefs ->
            prefs.remove(EXAM_START_EPOCH_DAY)
            prefs.remove(EXAM_END_EPOCH_DAY)
        }
    }
}

data class ExamPeriod(val start: LocalDate, val end: LocalDate) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)
}
