package com.unitrack.app.data.api

import com.unitrack.app.data.dto.ChecklistItemDto
import com.unitrack.app.data.dto.CreateChecklistItemRequest
import com.unitrack.app.data.dto.ReorderChecklistRequest
import com.unitrack.app.data.dto.TaskItemDto
import com.unitrack.app.data.dto.TaskSimpleResponse
import com.unitrack.app.data.dto.UpdateChecklistItemRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {

    @GET("tasks")
    suspend fun getTasks(
        @Query("status") status: String? = null,
        @Query("type") type: String? = null
    ): Response<List<TaskItemDto>>

    @GET("tasks/{id}")
    suspend fun getTaskById(@Path("id") id: String): Response<TaskItemDto>

    @POST("tasks/{id}/checklist")
    suspend fun addChecklistItem(
        @Path("id") taskId: String,
        @Body request: CreateChecklistItemRequest
    ): Response<ChecklistItemDto>

    @PUT("tasks/checklist/{itemId}")
    suspend fun updateChecklistItem(
        @Path("itemId") itemId: String,
        @Body request: UpdateChecklistItemRequest
    ): Response<ChecklistItemDto>

    @DELETE("tasks/checklist/{itemId}")
    suspend fun deleteChecklistItem(@Path("itemId") itemId: String): Response<TaskSimpleResponse>

    @PUT("tasks/{id}/checklist/reorder")
    suspend fun reorderChecklist(
        @Path("id") taskId: String,
        @Body request: ReorderChecklistRequest
    ): Response<List<ChecklistItemDto>>
}
