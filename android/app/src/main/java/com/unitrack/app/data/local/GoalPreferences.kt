package com.unitrack.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Diğer DataStore'larla aynı desen: delegate dosya seviyesinde tanımlı.
private val Context.goalPrefsStore by preferencesDataStore(name = "goal_prefs")

/**
 * Kullanıcının bu dönem için kendine koyduğu hedef GANO'yu tutar.
 *
 * Bilinçli olarak backend'e taşınmadı: bu tamamen kişisel, cihaza özel bir
 * hedef — sunucuya senkronize edilmesi gereken bir iş kuralı değil. Böylece
 * Stage 4'teki gerçek "GPA Simulator" (finalden X alırsam AGNO ne olur)
 * özelliğiyle karışmıyor; bu sadece dashboard'daki ilerleme çubuğunu
 * anlamlandıran basit bir hedef değeri.
 */
@Singleton
class GoalPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TARGET_GPA = doublePreferencesKey("target_gpa")
        const val DEFAULT_TARGET_GPA = 3.5
    }

    val targetGpa: Flow<Double> =
        context.goalPrefsStore.data.map { it[TARGET_GPA] ?: DEFAULT_TARGET_GPA }

    suspend fun setTargetGpa(value: Double) {
        context.goalPrefsStore.edit { prefs ->
            prefs[TARGET_GPA] = value.coerceIn(0.0, 4.0)
        }
    }
}
