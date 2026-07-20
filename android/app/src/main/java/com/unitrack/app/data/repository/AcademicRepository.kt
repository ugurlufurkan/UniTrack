package com.unitrack.app.data.repository

import com.unitrack.app.data.api.AcademicApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.api.requireSuccess
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.CreateCourseRequest
import com.unitrack.app.data.dto.DashboardDto
import com.unitrack.app.data.dto.GpaDto
import com.unitrack.app.data.dto.GradeBandDto
import com.unitrack.app.data.dto.GradeScaleDto
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.data.dto.SemesterRequest
import com.unitrack.app.data.dto.StatisticsDto
import com.unitrack.app.data.dto.TranscriptEntryDto
import com.unitrack.app.data.dto.UpdateCourseRequest
import com.unitrack.app.data.dto.UpdateGradeScaleRequest
import com.unitrack.app.data.local.CacheKeys
import com.unitrack.app.data.local.OfflineCache
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicRepository @Inject constructor(
    private val api: AcademicApiService,
    private val json: Json,
    private val cache: OfflineCache
) {
    suspend fun getDashboard(): DashboardDto =
        fetchWithCache(CacheKeys.DASHBOARD) { api.getDashboard().bodyOrThrow(json) }

    suspend fun getSemesters(): List<SemesterDto> =
        fetchWithCache(CacheKeys.SEMESTERS) { api.getSemesters().bodyOrThrow(json) }

    suspend fun createSemester(year: Int, term: String): SemesterDto =
        api.createSemester(SemesterRequest(year, term)).bodyOrThrow(json)

    suspend fun updateSemester(id: String, year: Int, term: String): SemesterDto =
        api.updateSemester(id, SemesterRequest(year, term)).bodyOrThrow(json)

    suspend fun deleteSemester(id: String) =
        api.deleteSemester(id).requireSuccess(json)

    suspend fun getCourses(): List<CourseDto> =
        fetchWithCache(CacheKeys.COURSES) { api.getCourses().bodyOrThrow(json) }

    suspend fun createCourse(request: CreateCourseRequest): CourseDto =
        api.createCourse(request).bodyOrThrow(json)

    suspend fun updateCourse(id: String, request: UpdateCourseRequest): CourseDto =
        api.updateCourse(id, request).bodyOrThrow(json)

    suspend fun deleteCourse(id: String) =
        api.deleteCourse(id).requireSuccess(json)

    suspend fun getGpa(): GpaDto =
        fetchWithCache(CacheKeys.GPA) { api.getGpa().bodyOrThrow(json) }

    suspend fun getStatistics(): StatisticsDto =
        fetchWithCache(CacheKeys.STATISTICS) { api.getStatistics().bodyOrThrow(json) }

    suspend fun getTranscript(): List<TranscriptEntryDto> =
        fetchWithCache(CacheKeys.TRANSCRIPT) { api.getTranscript().bodyOrThrow(json) }

    suspend fun getDefaultGradeScale(): GradeScaleDto =
        fetchWithCache(CacheKeys.GRADE_SCALE) { api.getDefaultGradeScale().bodyOrThrow(json) }

    /** gradeScale = null gönderilirse sistem varsayılanına dönülür. */
    suspend fun setDefaultGradeScale(gradeScale: List<GradeBandDto>?): GradeScaleDto =
        api.setDefaultGradeScale(UpdateGradeScaleRequest(gradeScale)).bodyOrThrow(json)

    /**
     * Okuma uçları için ortak "önce ağ, olmazsa cache" deseni.
     *
     * - Ağ isteği başarılı olursa: sonucu cache'e yazar ve döner.
     * - Ağa hiç ulaşılamazsa (IOException -> internet yok, timeout, DNS):
     *   son bilinen iyi veriyi cache'ten döner; cache de boşsa orijinal
     *   hatayı fırlatır.
     * - Sunucu bir HTTP hata cevabı dönerse (bodyOrThrow'un fırlattığı
     *   IllegalStateException gibi) bu, "gerçek" bir hatadır ve cache'e
     *   düşülmeden olduğu gibi yukarı fırlatılır — bayat veriyle
     *   gizlenmemelidir.
     */
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
