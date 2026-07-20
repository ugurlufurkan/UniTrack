package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// DASHBOARD  (GET /api/dashboard)
// ==========================

/**
 * Dashboard'daki devamsizlik ozeti - attendance modulunun kendi (daha
 * detayli) DTO'sundan kasitli olarak ayri ve daha sade tutuluyor; burada
 * yalnizca ana sayfadaki uyari satiri icin gereken alanlar var.
 * Json { ignoreUnknownKeys = true } sayesinde backend'in dondugu ek alanlar
 * (or. courses listesi) sorunsuzca yok sayilir.
 */
@Serializable
data class AttendanceOverviewSummaryDto(
    val totalCourses: Int = 0,
    val atRiskCourses: Int = 0,
    val averageAbsenceRate: Double = 0.0
)

@Serializable
data class DashboardDto(
    val totalSemesters: Int = 0,
    val totalCourses: Int = 0,
    val totalCredits: Int = 0,
    val passedCourses: Int = 0,
    val failedCourses: Int = 0,
    val ongoingCourses: Int = 0,
    val gpa: Double = 0.0,
    val weeklyLessonCount: Int = 0,
    val attendanceOverview: AttendanceOverviewSummaryDto? = null
)

// ==========================
// SEMESTER  (GET/POST/PUT/DELETE /api/semesters)
// ==========================
@Serializable
data class SemesterDto(
    val id: String = "",
    val userId: String = "",
    val year: Int = 0,
    val term: String = "",
    val createdAt: String? = null
)

@Serializable
data class SemesterRequest(
    val year: Int,
    val term: String
)

// ==========================
// GRADE SCALE (harf notu aralığı, "çan sistemi")
// ==========================
@Serializable
data class GradeBandDto(
    val letter: String = "",
    val min: Double = 0.0,
    val point: Double = 0.0
)

@Serializable
data class GradeScaleDto(
    val gradeScale: List<GradeBandDto> = emptyList(),
    val isCustom: Boolean = false
)

@Serializable
data class UpdateGradeScaleRequest(
    // null gönderilirse sistem varsayılanına dönülür.
    val gradeScale: List<GradeBandDto>?
)

// ==========================
// COURSE  (GET/POST/PUT/DELETE /api/courses)
// ==========================
@Serializable
data class CourseComponentDto(
    val id: String? = null,
    val name: String = "",
    val weight: Double = 0.0,
    val score: Double? = null
)

@Serializable
data class CourseDto(
    val id: String = "",
    val semesterId: String = "",
    val name: String = "",
    val credit: Int = 0,
    val components: List<CourseComponentDto> = emptyList(),
    val gradeScale: List<GradeBandDto>? = null,
    val average: Double? = null,
    val letterGrade: String? = null,
    val gradePoint: Double? = null,
    val passed: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class CreateCourseRequest(
    val semesterId: String,
    val name: String,
    val credit: Int,
    val components: List<CourseComponentDto>,
    val gradeScale: List<GradeBandDto>? = null
)

@Serializable
data class UpdateCourseRequest(
    val name: String? = null,
    val credit: Int? = null,
    val components: List<CourseComponentDto>? = null,
    val gradeScale: List<GradeBandDto>? = null
)

// ==========================
// GPA  (GET /api/gpa)
// ==========================
@Serializable
data class GpaCourseResult(
    val name: String = "",
    val credit: Int = 0,
    val average: Double? = null,
    val letter: String? = null,
    val point: Double? = null,
    val completed: Boolean = false
)

@Serializable
data class GpaDto(
    val courses: List<GpaCourseResult> = emptyList(),
    val gpa: Double = 0.0
)

// ==========================
// TRANSCRIPT  (GET /api/transcript)
// ==========================
@Serializable
data class TranscriptEntryDto(
    val semesterId: String = "",
    val course: String = "",
    val credit: Int = 0,
    val average: Double? = null,
    val letter: String = "",
    val point: Double? = null
)

// ==========================
// STATISTICS  (GET /api/statistics)
// ==========================
@Serializable
data class SemesterGpaDto(
    val semesterId: String = "",
    val semester: String = "",
    val gpa: Double = 0.0
)

@Serializable
data class StatisticsDto(
    val totalCourses: Int = 0,
    val totalCredits: Int = 0,
    val overallAverage: Double = 0.0,
    val passedCourses: Int = 0,
    val failedCourses: Int = 0,
    val ongoingCourses: Int = 0,
    val semesterGpa: List<SemesterGpaDto> = emptyList()
)

// ==========================
// Silme / genel başarı gövdesi
// ==========================
@Serializable
data class SimpleResponse(
    val success: Boolean = false,
    val message: String = ""
)

// ==========================
// Genel hata gövdesi (errorMiddleware'in döndürdüğü format)
// ==========================
@Serializable
data class ZodIssueDto(
    val message: String = ""
)

@Serializable
data class ErrorResponseDto(
    val success: Boolean = false,
    val message: String = "",
    val errors: List<ZodIssueDto>? = null
)