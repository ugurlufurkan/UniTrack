package com.unitrack.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Diğer DataStore'larla aynı desen: delegate dosya seviyesinde tanımlı.
private val Context.themeStore by preferencesDataStore(name = "theme_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Kullanıcının Panel'den elle seçtiği tema tercihi (Açık/Koyu/Sistem).
 *
 * Daha önce uygulama SADECE `isSystemInDarkTheme()`'e bağlıydı; kullanıcının
 * kendi seçimi hiç saklanmıyordu. Bu, cihazdadır (backend'e senkronize
 * edilmez) — ExamPeriodPreferences/GoalPreferences'la aynı gerekçeyle: bir
 * iş kuralı değil, tamamen kişisel bir görünüm tercihi.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeStore.data.map { prefs ->
        when (prefs[THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }
}
