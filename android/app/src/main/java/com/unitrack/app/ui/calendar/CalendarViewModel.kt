package com.unitrack.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.data.dto.CreateEventRequest
import com.unitrack.app.data.dto.CreateScheduleRequest
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.data.dto.EventNotificationDto
import com.unitrack.app.data.dto.UpdateEventRequest
import com.unitrack.app.data.dto.UpdateScheduleRequest
import com.unitrack.app.data.repository.AcademicRepository
import com.unitrack.app.data.repository.CalendarRepository
import com.unitrack.app.notifications.EventNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    // Ay görünümünde gösterilen ay, hafta görünümünde o haftayı içeren gün.
    val focusedDate: LocalDate = LocalDate.now(),
    val events: List<EventDto> = emptyList(),
    val schedule: List<CourseScheduleDto> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val academicRepository: AcademicRepository,
    private val notificationScheduler: EventNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _courses = MutableStateFlow<List<CourseDto>>(emptyList())
    val courses: StateFlow<List<CourseDto>> = _courses.asStateFlow()

    init {
        refresh()
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            runCatching { academicRepository.getCourses() }
                .onSuccess { _courses.value = it }
        }
    }


    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                val events = repository.getAllEvents()
                val schedule = repository.getSchedule()
                events to schedule
            }.onSuccess { (events, schedule) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    events = events,
                    schedule = schedule
                )
                // Bildirim zamanlaması best-effort: burada bir hata (ör. WorkManager
                // durumu, izin, cihaz kısıtlaması) çıkarsa Takvim ekranını çökertmemeli.
                runCatching { notificationScheduler.scheduleForEvents(events) }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Takvim yüklenemedi."
                )
            }
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setFocusedDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(focusedDate = date)
    }

    fun goToPreviousPeriod() {
        val current = _uiState.value
        val newDate = when (current.viewMode) {
            CalendarViewMode.MONTH -> current.focusedDate.minusMonths(1)
            CalendarViewMode.WEEK -> current.focusedDate.minusWeeks(1)
            else -> current.focusedDate
        }
        _uiState.value = current.copy(focusedDate = newDate)
    }

    fun goToNextPeriod() {
        val current = _uiState.value
        val newDate = when (current.viewMode) {
            CalendarViewMode.MONTH -> current.focusedDate.plusMonths(1)
            CalendarViewMode.WEEK -> current.focusedDate.plusWeeks(1)
            else -> current.focusedDate
        }
        _uiState.value = current.copy(focusedDate = newDate)
    }

    fun goToToday() {
        _uiState.value = _uiState.value.copy(focusedDate = LocalDate.now())
    }

    /** O tarihte gerçekleşen etkinlikler (startAt'ın tarih kısmı eşleşenler). */
    fun eventsOn(date: LocalDate): List<EventDto> =
        _uiState.value.events.filter { it.startAtLocalDateOrNull() == date }

    /** O tarihte, haftalık ders programından türeyen sanal "ders" işgalleri. */
    fun scheduleOn(date: LocalDate): List<CourseScheduleDto> {
        val backendDayOfWeek = date.dayOfWeek.toBackendDayOfWeek()
        return _uiState.value.schedule.filter { it.dayOfWeek == backendDayOfWeek }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ---------------------------------------------------------------
    // Etkinlik CRUD
    // ---------------------------------------------------------------

    fun createEvent(
        courseId: String?,
        title: String,
        description: String?,
        type: String,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        location: String?,
        priority: String,
        status: String,
        color: String,
        recurrence: String,
        notificationsEnabled: Boolean,
        notifications: List<EventNotificationDto>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.createEvent(
                    CreateEventRequest(
                        courseId = courseId,
                        title = title,
                        description = description,
                        type = type,
                        startAt = startAt.toIsoUtc(),
                        endAt = endAt?.toIsoUtc(),
                        location = location,
                        priority = priority,
                        status = status,
                        color = color,
                        recurrence = recurrence,
                        notificationsEnabled = notificationsEnabled,
                        notifications = notifications
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
                onResult(false)
            }
            _isSaving.value = false
        }
    }

    fun updateEvent(
        id: String,
        courseId: String?,
        title: String,
        description: String?,
        type: String,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        location: String?,
        priority: String,
        status: String,
        color: String,
        recurrence: String,
        notificationsEnabled: Boolean,
        notifications: List<EventNotificationDto>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.updateEvent(
                    id,
                    UpdateEventRequest(
                        courseId = courseId,
                        title = title,
                        description = description,
                        type = type,
                        startAt = startAt.toIsoUtc(),
                        endAt = endAt?.toIsoUtc(),
                        location = location,
                        priority = priority,
                        status = status,
                        color = color,
                        recurrence = recurrence,
                        notificationsEnabled = notificationsEnabled,
                        notifications = notifications
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
                onResult(false)
            }
            _isSaving.value = false
        }
    }

    fun updateEventStatus(id: String, status: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateEvent(id, UpdateEventRequest(status = status))
            }.onSuccess {
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteEvent(id)
            }.onSuccess {
                notificationScheduler.cancelForEvent(id)
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    // ---------------------------------------------------------------
    // Haftalık ders programı CRUD
    // ---------------------------------------------------------------

    fun createSchedule(
        courseId: String,
        dayOfWeek: Int,
        startTime: LocalTime,
        endTime: LocalTime,
        location: String?,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.createSchedule(
                    CreateScheduleRequest(
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.format(TIME_FORMATTER),
                        endTime = endTime.format(TIME_FORMATTER),
                        location = location
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
                onResult(false)
            }
            _isSaving.value = false
        }
    }

    fun updateSchedule(
        id: String,
        courseId: String,
        dayOfWeek: Int,
        startTime: LocalTime,
        endTime: LocalTime,
        location: String?,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.updateSchedule(
                    id,
                    UpdateScheduleRequest(
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.format(TIME_FORMATTER),
                        endTime = endTime.format(TIME_FORMATTER),
                        location = location
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
                onResult(false)
            }
            _isSaving.value = false
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteSchedule(id)
            }.onSuccess {
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message)
            }
        }
    }

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

// =====================================================================
// startAt/endAt <-> LocalDateTime yardımcıları
//
// Backend Zod tarafında `z.string().datetime()` bekliyor (ISO-8601, "Z"
// sonlu UTC). Telefonun yerel saat dilimini UTC'ye çevirip gönderiyoruz;
// geri okurken de aynı şekilde yerel saate çeviriyoruz ki kullanıcı hep
// kendi saatini görsün.
// =====================================================================

private val ISO_UTC_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

fun LocalDateTime.toIsoUtc(): String =
    this.atZone(java.time.ZoneId.systemDefault())
        .withZoneSameInstant(java.time.ZoneOffset.UTC)
        .toInstant()
        .let { ISO_UTC_FORMATTER.format(it) }

fun String.toLocalDateTimeOrNull(): LocalDateTime? = try {
    java.time.Instant.parse(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
} catch (e: Exception) {
    null
}

fun EventDto.startAtLocalDateTimeOrNull(): LocalDateTime? = startAt.toLocalDateTimeOrNull()

fun EventDto.startAtLocalDateOrNull(): LocalDate? = startAtLocalDateTimeOrNull()?.toLocalDate()

fun EventDto.endAtLocalDateTimeOrNull(): LocalDateTime? = endAt?.toLocalDateTimeOrNull()
