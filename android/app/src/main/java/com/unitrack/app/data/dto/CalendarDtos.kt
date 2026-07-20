package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// EVENT NOTIFICATIONS
// ==========================
@Serializable
data class EventNotificationDto(
    val id: String? = null,
    val daysBefore: Int = 0,
    val hoursBefore: Int = 0,
    val minutesBefore: Int = 0
)

// ==========================
// EVENT  (GET/POST/PUT/DELETE /api/v1/calendar/events)
// ==========================
@Serializable
data class EventDto(
    val id: String = "",
    val userId: String = "",
    val courseId: String? = null,
    // Bazı uçlar (ör. calendar/summary) join edilmiş ders adını da döner.
    val courseName: String? = null,
    val title: String = "",
    val description: String? = null,
    // lesson | exam | quiz | assignment | project | presentation | other
    val type: String = "other",
    val startAt: String = "",
    val endAt: String? = null,
    val location: String? = null,
    // low | medium | high
    val priority: String = "medium",
    // pending | in_progress | completed | cancelled
    val status: String = "pending",
    val color: String = "#6366F1",
    // none | daily | weekly | monthly
    val recurrence: String = "none",
    val notificationsEnabled: Boolean = true,
    val notifications: List<EventNotificationDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreateEventRequest(
    val courseId: String? = null,
    val title: String,
    val description: String? = null,
    val type: String,
    val startAt: String,
    val endAt: String? = null,
    val location: String? = null,
    val priority: String = "medium",
    val status: String = "pending",
    val color: String = "#6366F1",
    val recurrence: String = "none",
    val notificationsEnabled: Boolean = true,
    val notifications: List<EventNotificationDto> = emptyList()
)

@Serializable
data class UpdateEventRequest(
    val courseId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val location: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val color: String? = null,
    val recurrence: String? = null,
    val notificationsEnabled: Boolean? = null,
    val notifications: List<EventNotificationDto>? = null
)

// ==========================
// COURSE SCHEDULE  (haftalık ders programı)
// GET/POST/PUT/DELETE /api/v1/calendar/schedule
// ==========================
@Serializable
data class CourseScheduleDto(
    val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val courseName: String = "",
    // 0 = Pazar ... 6 = Cumartesi (backend ile birebir aynı)
    val dayOfWeek: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val location: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateScheduleRequest(
    val courseId: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val location: String? = null
)

@Serializable
data class UpdateScheduleRequest(
    val courseId: String? = null,
    val dayOfWeek: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null
)

// ==========================
// CALENDAR SUMMARY  (GET /api/v1/calendar/summary)
// Ana sayfadaki "Bugünkü Dersler / Yaklaşan Sınavlar / Yaklaşan Teslimler /
// Geciken Ödevler" kartları bu tek uçtan besleniyor.
// ==========================
@Serializable
data class CalendarSummaryDto(
    val todayClasses: List<CourseScheduleDto> = emptyList(),
    val todayLessons: List<EventDto> = emptyList(),
    val upcomingExams: List<EventDto> = emptyList(),
    val upcomingDeadlines: List<EventDto> = emptyList(),
    val overdueAssignments: List<EventDto> = emptyList()
)
