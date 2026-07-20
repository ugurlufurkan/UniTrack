package com.unitrack.app.data.api

import com.unitrack.app.data.dto.CalendarSummaryDto
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.data.dto.CreateEventRequest
import com.unitrack.app.data.dto.CreateScheduleRequest
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.data.dto.SimpleResponse
import com.unitrack.app.data.dto.UpdateEventRequest
import com.unitrack.app.data.dto.UpdateScheduleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApiService {

    @GET("calendar/summary")
    suspend fun getCalendarSummary(): Response<CalendarSummaryDto>

    @GET("calendar/events")
    suspend fun getEvents(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("type") type: String? = null
    ): Response<List<EventDto>>

    @GET("calendar/events/upcoming")
    suspend fun getUpcomingEvents(@Query("limit") limit: Int? = null): Response<List<EventDto>>

    @GET("calendar/events/{id}")
    suspend fun getEventById(@Path("id") id: String): Response<EventDto>

    @POST("calendar/events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<EventDto>

    @PUT("calendar/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: String,
        @Body request: UpdateEventRequest
    ): Response<EventDto>

    @DELETE("calendar/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): Response<SimpleResponse>

    @GET("calendar/schedule")
    suspend fun getSchedule(): Response<List<CourseScheduleDto>>

    @POST("calendar/schedule")
    suspend fun createSchedule(@Body request: CreateScheduleRequest): Response<CourseScheduleDto>

    @PUT("calendar/schedule/{id}")
    suspend fun updateSchedule(
        @Path("id") id: String,
        @Body request: UpdateScheduleRequest
    ): Response<CourseScheduleDto>

    @DELETE("calendar/schedule/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): Response<SimpleResponse>
}
