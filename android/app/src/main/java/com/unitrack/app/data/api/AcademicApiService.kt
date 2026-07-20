package com.unitrack.app.data.api

import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.CreateCourseRequest
import com.unitrack.app.data.dto.DashboardDto
import com.unitrack.app.data.dto.GpaDto
import com.unitrack.app.data.dto.GradeScaleDto
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.data.dto.SemesterRequest
import com.unitrack.app.data.dto.SimpleResponse
import com.unitrack.app.data.dto.StatisticsDto
import com.unitrack.app.data.dto.TranscriptEntryDto
import com.unitrack.app.data.dto.UpdateCourseRequest
import com.unitrack.app.data.dto.UpdateGradeScaleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AcademicApiService {

    @GET("dashboard")
    suspend fun getDashboard(): Response<DashboardDto>

    @GET("semesters")
    suspend fun getSemesters(): Response<List<SemesterDto>>

    @POST("semesters")
    suspend fun createSemester(@Body request: SemesterRequest): Response<SemesterDto>

    @PUT("semesters/{id}")
    suspend fun updateSemester(
        @Path("id") id: String,
        @Body request: SemesterRequest
    ): Response<SemesterDto>

    @DELETE("semesters/{id}")
    suspend fun deleteSemester(@Path("id") id: String): Response<SimpleResponse>

    @GET("courses")
    suspend fun getCourses(): Response<List<CourseDto>>

    @POST("courses")
    suspend fun createCourse(@Body request: CreateCourseRequest): Response<CourseDto>

    @PUT("courses/{id}")
    suspend fun updateCourse(
        @Path("id") id: String,
        @Body request: UpdateCourseRequest
    ): Response<CourseDto>

    @DELETE("courses/{id}")
    suspend fun deleteCourse(@Path("id") id: String): Response<SimpleResponse>

    @GET("gpa")
    suspend fun getGpa(): Response<GpaDto>

    @GET("statistics")
    suspend fun getStatistics(): Response<StatisticsDto>

    @GET("transcript")
    suspend fun getTranscript(): Response<List<TranscriptEntryDto>>

    @GET("grade-scale/default")
    suspend fun getDefaultGradeScale(): Response<GradeScaleDto>

    @PUT("grade-scale/default")
    suspend fun setDefaultGradeScale(
        @Body request: UpdateGradeScaleRequest
    ): Response<GradeScaleDto>
}