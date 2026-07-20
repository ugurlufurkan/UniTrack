package com.unitrack.app.data.repository

import com.unitrack.app.data.api.CalendarApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.api.requireSuccess
import com.unitrack.app.data.dto.CalendarSummaryDto
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.data.dto.CreateEventRequest
import com.unitrack.app.data.dto.CreateScheduleRequest
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.data.dto.UpdateEventRequest
import com.unitrack.app.data.dto.UpdateScheduleRequest
import com.unitrack.app.data.local.CacheKeys
import com.unitrack.app.data.local.OfflineCache
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Takvim modülü (etkinlikler, bildirimler, haftalık ders programı, ana sayfa
 * özeti) için tek repository. AcademicRepository'deki "önce ağ, olmazsa
 * cache" desenini birebir izliyor — bkz. oradaki fetchWithCache açıklaması.
 *
 * Yalnızca filtresiz/varsayılan sorgular cache'lenir (tüm etkinlikler, tüm
 * program, özet, yaklaşanlar). Tarih aralığı filtreli (ör. ay görünümü)
 * sorgular her ay farklı bir anahtar gerektireceğinden ve zaten filtresiz
 * "tüm etkinlikler" cache'i bir yedek olarak yeterli olduğundan doğrudan
 * ağdan okunur.
 */
@Singleton
class CalendarRepository @Inject constructor(
    private val api: CalendarApiService,
    private val json: Json,
    private val cache: OfflineCache
) {
    suspend fun getCalendarSummary(): CalendarSummaryDto =
        fetchWithCache(CacheKeys.CALENDAR_SUMMARY) { api.getCalendarSummary().bodyOrThrow(json) }

    /** Filtresiz tüm etkinlikler — Ay/Hafta/Liste görünümleri bunun üzerinden client-side filtrelenir. */
    suspend fun getAllEvents(): List<EventDto> =
        fetchWithCache(CacheKeys.CALENDAR_EVENTS_ALL) { api.getEvents().bodyOrThrow(json) }

    /** Tarih aralığı/tür filtreli sorgu — cache'lenmez, doğrudan ağdan okunur. */
    suspend fun getEvents(
        startDate: String? = null,
        endDate: String? = null,
        type: String? = null
    ): List<EventDto> =
        api.getEvents(startDate, endDate, type).bodyOrThrow(json)

    suspend fun getUpcomingEvents(limit: Int = 20): List<EventDto> =
        fetchWithCache(CacheKeys.CALENDAR_UPCOMING_EVENTS) {
            api.getUpcomingEvents(limit).bodyOrThrow(json)
        }

    suspend fun getEventById(id: String): EventDto =
        api.getEventById(id).bodyOrThrow(json)

    suspend fun createEvent(request: CreateEventRequest): EventDto =
        api.createEvent(request).bodyOrThrow(json)

    suspend fun updateEvent(id: String, request: UpdateEventRequest): EventDto =
        api.updateEvent(id, request).bodyOrThrow(json)

    suspend fun deleteEvent(id: String) =
        api.deleteEvent(id).requireSuccess(json)

    suspend fun getSchedule(): List<CourseScheduleDto> =
        fetchWithCache(CacheKeys.CALENDAR_SCHEDULE) { api.getSchedule().bodyOrThrow(json) }

    suspend fun createSchedule(request: CreateScheduleRequest): CourseScheduleDto =
        api.createSchedule(request).bodyOrThrow(json)

    suspend fun updateSchedule(id: String, request: UpdateScheduleRequest): CourseScheduleDto =
        api.updateSchedule(id, request).bodyOrThrow(json)

    suspend fun deleteSchedule(id: String) =
        api.deleteSchedule(id).requireSuccess(json)

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
