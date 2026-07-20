package com.unitrack.app.data.repository

import com.unitrack.app.data.api.TaskApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.api.requireSuccess
import com.unitrack.app.data.dto.ChecklistItemDto
import com.unitrack.app.data.dto.CreateChecklistItemRequest
import com.unitrack.app.data.dto.ReorderChecklistRequest
import com.unitrack.app.data.dto.TaskItemDto
import com.unitrack.app.data.dto.UpdateChecklistItemRequest
import com.unitrack.app.data.local.CacheKeys
import com.unitrack.app.data.local.OfflineCache
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Görev modülü (tasks) için repository. CalendarRepository'deki
 * "önce ağ, olmazsa cache" fetchWithCache deseni birebir izleniyor.
 *
 * Yalnızca filtresiz "tüm görevler" sorgusu cache'lenir. Filtreli
 * sorgular (tür/durum) her çağrıda doğrudan ağdan okunur.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val api: TaskApiService,
    private val json: Json,
    private val cache: OfflineCache
) {
    /** Filtresiz tüm görevler — client-side filtreleme için kullanılır. */
    suspend fun getAllTasks(): List<TaskItemDto> =
        fetchWithCache(CacheKeys.TASKS_ALL) { api.getTasks().bodyOrThrow(json) }

    /** Tür ve/veya durum filtreli sorgu — cache'lenmez, doğrudan ağdan okunur. */
    suspend fun getTasks(
        status: String? = null,
        type: String? = null
    ): List<TaskItemDto> =
        api.getTasks(status, type).bodyOrThrow(json)

    suspend fun getTaskById(id: String): TaskItemDto =
        api.getTaskById(id).bodyOrThrow(json)

    suspend fun addChecklistItem(taskId: String, title: String): ChecklistItemDto =
        api.addChecklistItem(taskId, CreateChecklistItemRequest(title)).bodyOrThrow(json)

    suspend fun updateChecklistItem(
        itemId: String,
        title: String? = null,
        isDone: Boolean? = null,
        sortOrder: Int? = null
    ): ChecklistItemDto =
        api.updateChecklistItem(
            itemId,
            UpdateChecklistItemRequest(title = title, isDone = isDone, sortOrder = sortOrder)
        ).bodyOrThrow(json)

    suspend fun deleteChecklistItem(itemId: String) =
        api.deleteChecklistItem(itemId).requireSuccess(json)

    suspend fun reorderChecklist(taskId: String, itemIds: List<String>): List<ChecklistItemDto> =
        api.reorderChecklist(taskId, ReorderChecklistRequest(itemIds)).bodyOrThrow(json)

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
