package com.unitrack.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.unitrack.app.data.api.CalendarApiService
import com.unitrack.app.data.api.TaskApiService
import com.unitrack.app.data.api.toUpdateBody
import com.unitrack.app.data.dto.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

class MutationBodyTest {
    @Test fun statusAndChecklistUpdatesDoNotSerializeUntouchedNullFields() = runBlocking {
        MockWebServer().use { server ->
            val json = Json { encodeDefaults = true }
            val retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
            server.enqueue(MockResponse().setBody("{}").setHeader("Content-Type", "application/json"))
            retrofit.create(CalendarApiService::class.java).updateEvent("event", UpdateEventRequest(status = "completed").toUpdateBody(json))
            assertEquals("""{"status":"completed"}""", server.takeRequest().body.readUtf8())
            server.enqueue(MockResponse().setBody("{}").setHeader("Content-Type", "application/json"))
            retrofit.create(TaskApiService::class.java).updateChecklistItem("item", UpdateChecklistItemRequest(isDone = false, sortOrder = 0).toUpdateBody(json))
            val checklist = json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals(setOf("isDone", "sortOrder"), checklist.keys)
            assertEquals(JsonPrimitive(false), checklist["isDone"])
            assertEquals(JsonPrimitive(0), checklist["sortOrder"])
        }
    }

    @Test fun formUpdatesCanExplicitlyClearNullableFieldsWithoutNullingRequiredFields() {
        val json = Json { encodeDefaults = true }
        val event = UpdateEventRequest(title = "Exam", courseId = null).toUpdateBody(json, setOf("courseId", "description", "endAt", "location"))
        assertEquals(JsonNull, event["courseId"])
        assertFalse("status" in event)
        assertFalse("notifications" in event)
        val course = UpdateCourseRequest(credit = 3, gradeScale = null).toUpdateBody(json, setOf("gradeScale"))
        assertEquals(setOf("credit", "gradeScale"), course.keys)
        assertEquals(JsonNull, course["gradeScale"])
        val attendance = UpdateCourseAttendanceSettingsRequest(attendanceLimitHours = 0).toUpdateBody(json)
        assertEquals(setOf("attendanceLimitHours"), attendance.keys)
        assertEquals(JsonPrimitive(0), attendance["attendanceLimitHours"])
    }
}
