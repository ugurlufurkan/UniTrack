package com.unitrack.app.data.api

import com.unitrack.app.data.dto.CreateChecklistItemRequest
import com.unitrack.app.data.dto.CreateTaskRequest
import com.unitrack.app.data.dto.TaskDto
import com.unitrack.app.data.dto.UpdateChecklistItemRequest
import com.unitrack.app.data.dto.UpdateTaskRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {

    @GET("tasks")
    suspend fun getAll(
        @Query("status") status: String? = null,
        @Query("type") type: String? = null,
        @Query("courseId") courseId: String? = null
    ): Response<List<TaskDto>>

    @GET("tasks/{taskId}")
    suspend fun getById(@Path("taskId") taskId: String): Response<TaskDto>

    @POST("tasks")
    suspend fun create(@Body request: CreateTaskRequest): Response<TaskDto>

    @PUT("tasks/{taskId}")
    suspend fun update(
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): Response<TaskDto>

    @DELETE("tasks/{taskId}")
    suspend fun delete(@Path("taskId") taskId: String): Response<Unit>

    @PATCH("tasks/{taskId}/toggle")
    suspend fun toggleStatus(@Path("taskId") taskId: String): Response<TaskDto>

    @POST("tasks/{taskId}/checklist")
    suspend fun addChecklistItem(
        @Path("taskId") taskId: String,
        @Body request: CreateChecklistItemRequest
    ): Response<TaskDto>

    @PUT("tasks/{taskId}/checklist/{itemId}")
    suspend fun updateChecklistItem(
        @Path("taskId") taskId: String,
        @Path("itemId") itemId: String,
        @Body request: UpdateChecklistItemRequest
    ): Response<TaskDto>

    @DELETE("tasks/{taskId}/checklist/{itemId}")
    suspend fun removeChecklistItem(
        @Path("taskId") taskId: String,
        @Path("itemId") itemId: String
    ): Response<TaskDto>
}
