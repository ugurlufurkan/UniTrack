package com.unitrack.app.data.repository

import com.unitrack.app.data.api.TaskApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.dto.CreateChecklistItemRequest
import com.unitrack.app.data.dto.CreateTaskRequest
import com.unitrack.app.data.dto.TaskDto
import com.unitrack.app.data.dto.UpdateChecklistItemRequest
import com.unitrack.app.data.dto.UpdateTaskRequest
import com.unitrack.app.data.local.CacheKeys
import com.unitrack.app.data.local.OfflineCache
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: TaskApiService,
    private val json: Json,
    private val cache: OfflineCache
) {
    suspend fun getAll(
        status: String? = null,
        type: String? = null,
        courseId: String? = null
    ): List<TaskDto> =
        fetchWithCache(CacheKeys.tasksFiltered(status, type, courseId)) {
            api.getAll(status, type, courseId).bodyOrThrow(json)
        }

    suspend fun getById(taskId: String): TaskDto =
        api.getById(taskId).bodyOrThrow(json)

    suspend fun create(
        title: String,
        courseId: String? = null,
        description: String? = null,
        type: String? = null,
        dueAt: String? = null,
        priority: String? = null,
        checklist: List<String> = emptyList()
    ): TaskDto = api.create(
        CreateTaskRequest(
            courseId = courseId,
            title = title,
            description = description,
            type = type,
            dueAt = dueAt,
            priority = priority,
            checklist = checklist.map { CreateChecklistItemRequest(it) }.ifEmpty { null }
        )
    ).bodyOrThrow(json)

    suspend fun update(
        taskId: String,
        courseId: String? = null,
        title: String? = null,
        description: String? = null,
        type: String? = null,
        dueAt: String? = null,
        priority: String? = null,
        status: String? = null
    ): TaskDto = api.update(
        taskId,
        UpdateTaskRequest(courseId, title, description, type, dueAt, priority, status)
    ).bodyOrThrow(json)

    suspend fun delete(taskId: String) {
        api.delete(taskId)
    }

    suspend fun toggleStatus(taskId: String): TaskDto =
        api.toggleStatus(taskId).bodyOrThrow(json)

    suspend fun addChecklistItem(taskId: String, title: String): TaskDto =
        api.addChecklistItem(taskId, CreateChecklistItemRequest(title)).bodyOrThrow(json)

    suspend fun updateChecklistItem(
        taskId: String,
        itemId: String,
        title: String? = null,
        isDone: Boolean? = null,
        sortOrder: Int? = null
    ): TaskDto = api.updateChecklistItem(
        taskId, itemId, UpdateChecklistItemRequest(title, isDone, sortOrder)
    ).bodyOrThrow(json)

    suspend fun removeChecklistItem(taskId: String, itemId: String): TaskDto =
        api.removeChecklistItem(taskId, itemId).bodyOrThrow(json)

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
