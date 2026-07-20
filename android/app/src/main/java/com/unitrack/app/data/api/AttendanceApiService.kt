package com.unitrack.app.data.api

import com.unitrack.app.data.dto.AttendanceCourseDetailDto
import com.unitrack.app.data.dto.AttendanceCourseSummaryDto
import com.unitrack.app.data.dto.AttendanceOverviewDto
import com.unitrack.app.data.dto.AttendanceRecordDto
import com.unitrack.app.data.dto.UpdateAttendanceRecordRequest
import com.unitrack.app.data.dto.UpdateCourseAttendanceSettingsRequest
import com.unitrack.app.data.dto.UpsertAttendanceRecordRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AttendanceApiService {

    @GET("attendance/overview")
    suspend fun getOverview(): Response<AttendanceOverviewDto>

    @GET("attendance/courses")
    suspend fun getCourses(): Response<List<AttendanceCourseSummaryDto>>

    @GET("attendance/courses/{courseId}")
    suspend fun getCourseDetail(
        @Path("courseId") courseId: String
    ): Response<AttendanceCourseDetailDto>

    @POST("attendance/courses/{courseId}/records")
    suspend fun upsertRecord(
        @Path("courseId") courseId: String,
        @Body request: UpsertAttendanceRecordRequest
    ): Response<AttendanceRecordDto>

    @PUT("attendance/records/{id}")
    suspend fun updateRecord(
        @Path("id") id: String,
        @Body request: UpdateAttendanceRecordRequest
    ): Response<AttendanceRecordDto>

    @DELETE("attendance/records/{id}")
    suspend fun deleteRecord(
        @Path("id") id: String
    ): Response<Unit>

    @PUT("attendance/courses/{courseId}/settings")
    suspend fun updateCourseSettings(
        @Path("courseId") courseId: String,
        @Body request: UpdateCourseAttendanceSettingsRequest
    ): Response<Unit>
}
