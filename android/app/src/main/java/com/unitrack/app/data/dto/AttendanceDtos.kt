package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// ATTENDANCE SUMMARY  (GET attendance/overview, GET attendance/courses)
// Saat bazli: her ders haftada weeklyHours saat, devamsizlik limiti de
// (attendanceLimitHours) toplam saat cinsinden ayarlanabilir.
// ==========================
@Serializable
data class AttendanceCourseSummaryDto(
    val courseId: String = "",
    val courseName: String = "",
    val totalWeeks: Int = 0,
    val weeklyHours: Int = 0,
    val totalCourseHours: Int = 0,
    val attendedHours: Int = 0,
    val absentHours: Int = 0,
    val excusedHours: Int = 0,
    val unmarkedWeeks: Int = 0,
    val absenceRatePercent: Double = 0.0,
    // 0 = henuz ayarlanmadi (limit yok)
    val attendanceLimitHours: Int = 0,
    val remainingAllowedHours: Int? = null,
    val isAtRisk: Boolean = false
)

@Serializable
data class AttendanceOverviewDto(
    val totalCourses: Int = 0,
    val atRiskCourses: Int = 0,
    val averageAbsenceRate: Double = 0.0,
    val courses: List<AttendanceCourseSummaryDto> = emptyList()
)

// ==========================
// ATTENDANCE RECORD  (embedded in course detail)
// Bir haftaya ait kayit: kac saat katildi / katilmadi / izinliydi.
// ==========================
@Serializable
data class AttendanceRecordDto(
    val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val weekNumber: Int = 0,
    val date: String = "",
    val attendedHours: Int = 0,
    val absentHours: Int = 0,
    val excusedHours: Int = 0,
    val note: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ==========================
// COURSE DETAIL  (GET attendance/courses/{courseId})
// Summary + records list
// ==========================
@Serializable
data class AttendanceCourseDetailDto(
    val courseId: String = "",
    val courseName: String = "",
    val totalWeeks: Int = 0,
    val weeklyHours: Int = 0,
    val totalCourseHours: Int = 0,
    val attendedHours: Int = 0,
    val absentHours: Int = 0,
    val excusedHours: Int = 0,
    val unmarkedWeeks: Int = 0,
    val absenceRatePercent: Double = 0.0,
    val attendanceLimitHours: Int = 0,
    val remainingAllowedHours: Int? = null,
    val isAtRisk: Boolean = false,
    val records: List<AttendanceRecordDto> = emptyList()
)

// ==========================
// REQUESTS
// ==========================
@Serializable
data class UpsertAttendanceRecordRequest(
    val weekNumber: Int,
    val date: String,
    val attendedHours: Int,
    val absentHours: Int,
    val excusedHours: Int,
    val note: String? = null
)

@Serializable
data class UpdateAttendanceRecordRequest(
    val date: String? = null,
    val attendedHours: Int? = null,
    val absentHours: Int? = null,
    val excusedHours: Int? = null,
    val note: String? = null
)

@Serializable
data class UpdateCourseAttendanceSettingsRequest(
    val totalWeeks: Int? = null,
    val weeklyHours: Int? = null,
    val attendanceLimitHours: Int? = null
)
