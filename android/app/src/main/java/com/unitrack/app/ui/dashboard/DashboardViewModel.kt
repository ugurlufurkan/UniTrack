package com.unitrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.local.ExamPeriod
import com.unitrack.app.data.local.ExamPeriodPreferences
import com.unitrack.app.data.local.GoalPreferences
import com.unitrack.app.data.dto.CalendarSummaryDto
import com.unitrack.app.data.repository.AcademicRepository
import com.unitrack.app.data.repository.CalendarRepository
import com.unitrack.app.data.repository.SettingsSyncRepository
import com.unitrack.app.notifications.EventNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AcademicRepository,
    private val calendarRepository: CalendarRepository,
    private val notificationScheduler: EventNotificationScheduler,
    private val goalPreferences: GoalPreferences,
    private val examPeriodPreferences: ExamPeriodPreferences,
    private val settingsSyncRepository: SettingsSyncRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    // Ana sayfadaki "Bugünkü Dersler / Yaklaşan Sınav / Yaklaşan Teslim /
    // Geciken Ödev" kartları için — takvim modülü henüz kullanılmamışsa
    // (ör. hiç etkinlik yoksa) null kalabilir, dashboard'un geri kalanını
    // bloke etmemesi için ayrı ve best-effort bir state.
    private val _calendarSummary = MutableStateFlow<CalendarSummaryDto?>(null)
    val calendarSummary: StateFlow<CalendarSummaryDto?> = _calendarSummary.asStateFlow()

    // Kullanıcının kendine koyduğu hedef GANO (cihazda saklanır, bkz. GoalPreferences).
    // GPA hero kartındaki ilerleme çubuğu bunu referans alır.
    val targetGpa: StateFlow<Double> = goalPreferences.targetGpa.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalPreferences.DEFAULT_TARGET_GPA
    )

    fun setTargetGpa(value: Double) {
        viewModelScope.launch {
            goalPreferences.setTargetGpa(value)
            settingsSyncRepository.pushTargetGpa(value)
        }
    }

    // Kullanıcının kendi girdiği sınav haftası (bkz. ExamPeriodPreferences).
    // AmbientBackground'daki amber ton bunu referans alır (AmbientMoodViewModel
    // üzerinden, ayrı bir okuma — burada sadece DÜZENLEME arayüzü için tutuluyor).
    val examPeriod: StateFlow<ExamPeriod?> = examPeriodPreferences.examPeriod.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setExamPeriod(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            examPeriodPreferences.setExamPeriod(start, end)
            settingsSyncRepository.pushExamPeriod(start, end)
        }
    }

    fun clearExamPeriod() {
        viewModelScope.launch {
            examPeriodPreferences.clearExamPeriod()
            settingsSyncRepository.pushExamPeriodCleared()
        }
    }

    // Pull-to-refresh ve ON_RESUME tazelemesi için ayrı bir bayrak: bunlar
    // sırasında uiState Success'te kalır (eski veri ekranda görünmeye devam
    // eder), sadece üstte dönen bir gösterge olur.
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Arka planda yapılan tazeleme başarısız olursa ekrandaki veriyi ERROR
    // state'ine çevirip kaybettirmek yerine tek seferlik bir Snackbar mesajı
    // olarak gösteriyoruz.
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    /** İlk yükleme veya "Tekrar Dene" ile tetiklenen tam ekran yükleme. */
    fun load() {

        viewModelScope.launch {

            _uiState.value = DashboardUiState.Loading

            runCatching {
                repository.getDashboard()
            }.onSuccess {

                _uiState.value =
                    DashboardUiState.Success(it)

            }.onFailure {

                _uiState.value =
                    DashboardUiState.Error(
                        it.message ?: "Dashboard yüklenemedi."
                    )

            }

            loadCalendarSummary()

        }

    }

    /** Takvim özeti best-effort yüklenir — başarısız olursa dashboard'un geri kalanını etkilemez. */
    private fun loadCalendarSummary() {
        viewModelScope.launch {
            runCatching {
                calendarRepository.getCalendarSummary()
            }.onSuccess {
                _calendarSummary.value = it
            }
        }

        // Kullanıcı hiç Takvim sekmesini açmasa bile hatırlatmaların zamanlı
        // kalması için: her dashboard yüklemesinde tüm etkinlikler çekilip
        // EventNotificationScheduler'a veriliyor (bkz. CalendarViewModel'deki
        // aynı çağrı — burada olmayan tek fark, bunun sessizce/best-effort
        // çalışması).
        viewModelScope.launch {
            runCatching {
                calendarRepository.getAllEvents()
            }.onSuccess { events ->
                // Bildirim zamanlaması best-effort: bir hata çıkarsa Dashboard'u
                // (veya ilerideki bir ekranı) çökertmemeli.
                runCatching { notificationScheduler.scheduleForEvents(events) }
            }
        }
    }

    /**
     * Pull-to-refresh ve ekran ON_RESUME olduğunda çağrılır. Mevcut veri
     * halihazırda ekrandaysa (Success) onu korur; yalnızca henüz hiç veri
     * yoksa (Loading/Error state'inde) tam yüklemeye düşer.
     */
    fun refresh() {

        if (_uiState.value !is DashboardUiState.Success) {
            load()
            return
        }

        viewModelScope.launch {

            _isRefreshing.value = true

            runCatching {
                repository.getDashboard()
            }.onSuccess {

                _uiState.value = DashboardUiState.Success(it)
                _refreshError.value = null

            }.onFailure {

                _refreshError.value =
                    it.message ?: "Tazelenemedi, eski veriler gösteriliyor."

            }

            loadCalendarSummary()

            _isRefreshing.value = false

        }

    }

    fun clearRefreshError() {
        _refreshError.value = null
    }

}
