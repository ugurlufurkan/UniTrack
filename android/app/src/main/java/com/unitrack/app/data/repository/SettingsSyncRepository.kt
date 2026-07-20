package com.unitrack.app.data.repository

import com.unitrack.app.data.api.SettingsApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.dto.SettingsUpdateRequest
import com.unitrack.app.data.local.ExamPeriodPreferences
import com.unitrack.app.data.local.GoalPreferences
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.data.local.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tema / hedef GANO / sınav haftası ayarlarının cihaz (DataStore) ile
 * backend (users tablosu) arasında senkronizasyonu.
 *
 * Neden ayrı bir repository (ThemePreferences vb. içine gömülmedi)?
 * ThemePreferences/GoalPreferences/ExamPeriodPreferences saf, ağdan
 * habersiz yerel depolar olarak kalsın istedik — SettingsApiService'i
 * onlara enjekte edersek "yerel ayar sınıfı ağ çağrısı yapıyor" gibi tuhaf
 * bir bağımlılık yönü oluşurdu. Bunun yerine bu repository ikisinin de
 * üstünde durup ikisini birbirine bağlıyor.
 *
 * Push (cihaz -> sunucu) mantığı: backend PATCH'i gövdedeki TÜM alanları
 * (encodeDefaults=true) bekler, bu yüzden her push'ta üç ayarın da güncel
 * halini yerelden okuyup TAM bir SettingsUpdateRequest gönderiyoruz — aksi
 * halde örn. sadece tema değiştirince hedef GANO sunucuda null'a düşerdi.
 */
@Singleton
class SettingsSyncRepository @Inject constructor(
    private val api: SettingsApiService,
    private val json: Json,
    private val themePreferences: ThemePreferences,
    private val goalPreferences: GoalPreferences,
    private val examPeriodPreferences: ExamPeriodPreferences
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Girişten sonra (veya yeni bir cihazda açılışta) çağrılır: sunucudaki
     * ayarları yerel DataStore'lara yazar, böylece "telefon değişince
     * ayarlarım geri gelsin" gerçekleşir.
     *
     * Sunucuda null olan alanlara dokunulmaz (kullanıcı o cihazda zaten bir
     * şey seçmiş olabilir, sunucunun "hiç ayarlanmamış" durumu onu ezmesin).
     */
    suspend fun syncFromServer() {
        val settings = api.getSettings().bodyOrThrow(json)

        settings.themePreference?.let { raw ->
            val mode = runCatching { ThemeMode.valueOf(raw) }.getOrNull()
            if (mode != null) themePreferences.setThemeMode(mode)
        }

        settings.targetGpa?.let { goalPreferences.setTargetGpa(it) }

        val start = settings.examPeriodStart?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val end = settings.examPeriodEnd?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (start != null && end != null) {
            examPeriodPreferences.setExamPeriod(start, end)
        }
    }

    suspend fun pushThemeMode(mode: ThemeMode) =
        pushCurrentState(themeOverride = mode)

    suspend fun pushTargetGpa(value: Double) =
        pushCurrentState(targetGpaOverride = value)

    suspend fun pushExamPeriod(start: LocalDate, end: LocalDate) =
        pushCurrentState(examPeriodOverride = start to end)

    suspend fun pushExamPeriodCleared() =
        pushCurrentState(clearExamPeriod = true)

    /**
     * Üç ayarın da güncel değerini yerelden okuyup (gerekirse tek bir alanı
     * override ederek) sunucuya tam obje olarak gönderir. Ağ hatası
     * kullanıcının işlemini bozmasın diye burada yutulur (best-effort);
     * bir sonraki push veya syncFromServer eninde sonunda tutarlılığı
     * sağlar.
     */
    private suspend fun pushCurrentState(
        themeOverride: ThemeMode? = null,
        targetGpaOverride: Double? = null,
        examPeriodOverride: Pair<LocalDate, LocalDate>? = null,
        clearExamPeriod: Boolean = false
    ) {
        runCatching {
            val theme = themeOverride ?: themePreferences.themeMode.first()
            val target = targetGpaOverride ?: goalPreferences.targetGpa.first()
            val exam = when {
                clearExamPeriod -> null
                examPeriodOverride != null -> examPeriodOverride
                else -> examPeriodPreferences.examPeriod.first()?.let { it.start to it.end }
            }

            api.updateSettings(
                SettingsUpdateRequest(
                    themePreference = theme.name,
                    targetGpa = target,
                    examPeriodStart = exam?.first?.format(dateFormatter),
                    examPeriodEnd = exam?.second?.format(dateFormatter)
                )
            ).bodyOrThrow(json)
        }
    }
}
