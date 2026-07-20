package com.unitrack.app.data.repository

import com.unitrack.app.data.api.AttendanceApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.api.requireSuccess
import com.unitrack.app.data.dto.AttendanceCourseDetailDto
import com.unitrack.app.data.dto.AttendanceCourseSummaryDto
import com.unitrack.app.data.dto.AttendanceOverviewDto
import com.unitrack.app.data.dto.AttendanceRecordDto
import com.unitrack.app.data.dto.UpdateAttendanceRecordRequest
import com.unitrack.app.data.dto.UpdateCourseAttendanceSettingsRequest
import com.unitrack.app.data.dto.UpsertAttendanceRecordRequest
import com.unitrack.app.data.local.CacheKeys
import com.unitrack.app.data.local.OfflineCache
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Devamsızlık modülü için tek repository (saat bazlı).
 * CalendarRepository'deki "önce ağ, olmazsa cache" desenini birebir izliyor.
 * Yalnızca iki özet GET ucu cache'lenir (overview, course detail);
 * yazma işlemleri (upsert/update/delete) hiç cache'lenmez.
 */
@Singleton
class AttendanceRepository @Inject constructor(
    private val api: AttendanceApiService,
    private val json: Json,
    private val cache: OfflineCache
) {
    /** Genel devamsızlık özeti — tüm dersler ve istatistikler. */
    suspend fun getOverview(): AttendanceOverviewDto =
        fetchWithCache(CacheKeys.ATTENDANCE_OVERVIEW) { api.getOverview().bodyOrThrow(json) }

    /** Devamsızlık takibi yapılan ders listesi. */
    suspend fun getCourses(): List<AttendanceCourseSummaryDto> =
        fetchWithCache(CacheKeys.ATTENDANCE_COURSES) { api.getCourses().bodyOrThrow(json) }

    /** Tek dersin devamsızlık detayı + kayıt listesi. */
    suspend fun getCourseDetail(courseId: String): AttendanceCourseDetailDto =
        fetchWithCache("${CacheKeys.ATTENDANCE_COURSE_DETAIL_PREFIX}$courseId") {
            api.getCourseDetail(courseId).bodyOrThrow(json)
        }

    /** Bir hafta için saat bazlı devamsızlık kaydı ekler ya da üzerine yazar (upsert). */
    suspend fun upsertRecord(
        courseId: String,
        request: UpsertAttendanceRecordRequest
    ): AttendanceRecordDto =
        api.upsertRecord(courseId, request).bodyOrThrow(json)

    /** Mevcut bir kaydı doğrudan id üzerinden günceller. */
    suspend fun updateRecord(
        id: String,
        request: UpdateAttendanceRecordRequest
    ): AttendanceRecordDto =
        api.updateRecord(id, request).bodyOrThrow(json)

    /** Bir haftanın işaretini tamamen siler (→ "işaretlenmedi" durumuna döner). */
    suspend fun deleteRecord(id: String) =
        api.deleteRecord(id).requireSuccess(json)

    /** Dersin hafta sayısını, haftalık saatini ve/veya devamsızlık limitini (saat) günceller. */
    suspend fun updateCourseSettings(
        courseId: String,
        totalWeeks: Int? = null,
        weeklyHours: Int? = null,
        attendanceLimitHours: Int? = null
    ) = api.updateCourseSettings(
        courseId,
        UpdateCourseAttendanceSettingsRequest(
            totalWeeks = totalWeeks,
            weeklyHours = weeklyHours,
            attendanceLimitHours = attendanceLimitHours
        )
    ).requireSuccess(json)

    private suspend inline fun <reified T> fetchWithCache(
        key: String,
        network: suspend () -> T
    ): T {
        return try {
            val fresh = network()
            cache.save(key, fresh)
            fresh
        } catch (e: IOException) {
            cache.load<T>(key) ?: throw e
        }
    }
}
