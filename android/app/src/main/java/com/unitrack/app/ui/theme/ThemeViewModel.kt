package com.unitrack.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.data.local.ThemePreferences
import com.unitrack.app.data.repository.SettingsSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity'nin kök seviyesinde (AmbientMoodViewModel'le aynı seviyede)
 * kullanılır — UniTrackTheme'e verilecek `darkTheme` parametresinin tek
 * kaynağı burasıdır. Ekranlar arasında paylaşılan tercih, aşağı doğru
 * (DashboardScreen'deki seçiciye kadar) parametre olarak taşınır; hiçbir
 * alt ekran doğrudan ThemePreferences'a bağlanmaz.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val settingsSyncRepository: SettingsSyncRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            // En iyi çaba (best-effort): sunucuya yansıtılamazsa bile
            // cihazdaki tercih zaten kaydedildi, kullanıcı bunu fark etmez.
            settingsSyncRepository.pushThemeMode(mode)
        }
    }
}
