package com.unitrack.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.AttendanceCourseDetailDto
import com.unitrack.app.data.dto.AttendanceOverviewDto
import com.unitrack.app.data.dto.UpsertAttendanceRecordRequest
import com.unitrack.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// =====================================================================
// UI State
// =====================================================================

sealed class AttendanceUiState {
    object Loading : AttendanceUiState()
    data class Success(val overview: AttendanceOverviewDto) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
}

sealed class CourseAttendanceUiState {
    object Loading : CourseAttendanceUiState()
    data class Success(val detail: AttendanceCourseDetailDto) : CourseAttendanceUiState()
    data class Error(val message: String) : CourseAttendanceUiState()
}

// =====================================================================
// ViewModel
// =====================================================================

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {

    // --- Overview / list screen ---
    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    // --- Single course detail screen ---
    private val _courseState = MutableStateFlow<CourseAttendanceUiState>(CourseAttendanceUiState.Loading)
    val courseState: StateFlow<CourseAttendanceUiState> = _courseState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        refresh()
    }

    // ---------------------------------------------------------------
    // Overview screen
    // ---------------------------------------------------------------

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.Loading
            runCatching { repository.getOverview() }
                .onSuccess { _uiState.value = AttendanceUiState.Success(it) }
                .onFailure {
                    _uiState.value = AttendanceUiState.Error(
                        it.message ?: "Devamsızlık verisi yüklenemedi."
                    )
                }
        }
    }

    // ---------------------------------------------------------------
    // Course detail screen
    // ---------------------------------------------------------------

    fun loadCourse(courseId: String) {
        viewModelScope.launch {
            _courseState.value = CourseAttendanceUiState.Loading
            runCatching { repository.getCourseDetail(courseId) }
                .onSuccess { _courseState.value = CourseAttendanceUiState.Success(it) }
                .onFailure {
                    _courseState.value = CourseAttendanceUiState.Error(
                        it.message ?: "Ders devamsızlık bilgisi yüklenemedi."
                    )
                }
        }
    }

    /**
     * Bir hafta için saat bazlı devamsızlık kaydı koyar/günceller
     * (kaç saat katıldı / katılmadı / izinliydi).
     */
    fun markWeekHours(
        courseId: String,
        weekNumber: Int,
        attendedHours: Int,
        absentHours: Int,
        excusedHours: Int,
        existingDate: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val dateStr = existingDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00:00.000Z"
            runCatching {
                repository.upsertRecord(
                    courseId,
                    UpsertAttendanceRecordRequest(
                        weekNumber = weekNumber,
                        date = dateStr,
                        attendedHours = attendedHours,
                        absentHours = absentHours,
                        excusedHours = excusedHours
                    )
                )
            }.onSuccess {
                // Refresh course detail so UI reflects the new record
                runCatching { repository.getCourseDetail(courseId) }
                    .onSuccess { detail ->
                        _courseState.value = CourseAttendanceUiState.Success(detail)
                    }
                onDone()
            }.onFailure { err ->
                val current = _courseState.value
                if (current is CourseAttendanceUiState.Success) {
                    _courseState.value = CourseAttendanceUiState.Error(
                        err.message ?: "Kayıt güncellenemedi."
                    )
                }
            }
            _isSaving.value = false
        }
    }

    /** Bir haftanın işaretini tamamen siler. */
    fun deleteRecord(courseId: String, recordId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching { repository.deleteRecord(recordId) }
                .onSuccess {
                    runCatching { repository.getCourseDetail(courseId) }
                        .onSuccess { detail ->
                            _courseState.value = CourseAttendanceUiState.Success(detail)
                        }
                }
                .onFailure { err ->
                    // best-effort — swallow silently on delete failure
                    _courseState.value = CourseAttendanceUiState.Error(
                        err.message ?: "Kayıt silinemedi."
                    )
                }
            _isSaving.value = false
        }
    }

    /**
     * Dersin toplam hafta sayısını, haftalık ders saatini ve/veya devamsızlık
     * limitini (saat) günceller, ardından detayı yeniden yükler.
     * Null geçilen alanlar değişmeden kalır.
     */
    fun updateSettings(
        courseId: String,
        totalWeeks: Int? = null,
        weeklyHours: Int? = null,
        attendanceLimitHours: Int? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.updateCourseSettings(courseId, totalWeeks, weeklyHours, attendanceLimitHours)
            }
                .onSuccess {
                    runCatching { repository.getCourseDetail(courseId) }
                        .onSuccess { detail ->
                            _courseState.value = CourseAttendanceUiState.Success(detail)
                        }
                    onDone()
                }
                .onFailure { err ->
                    _courseState.value = CourseAttendanceUiState.Error(
                        err.message ?: "Ayarlar güncellenemedi."
                    )
                }
            _isSaving.value = false
        }
    }

    fun clearCourseError(courseId: String) {
        loadCourse(courseId)
    }
}
