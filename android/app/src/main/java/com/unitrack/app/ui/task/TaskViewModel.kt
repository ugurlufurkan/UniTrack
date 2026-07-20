package com.unitrack.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.TaskItemDto
import com.unitrack.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------
// UI State
// ---------------------------------------------------------------

sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Error(val message: String) : TaskUiState()
    data class Success(val tasks: List<TaskItemDto>) : TaskUiState()
}

// ---------------------------------------------------------------
// Filter models
// ---------------------------------------------------------------

enum class TaskTypeFilter(val apiValue: String?, val label: String) {
    ALL(null, "Tümü"),
    ASSIGNMENT("assignment", "Ödev"),
    PROJECT("project", "Proje"),
    PRESENTATION("presentation", "Sunum")
}

enum class TaskStatusFilter(val apiValue: String?, val label: String) {
    ALL(null, "Tümü"),
    PENDING("pending", "Bekliyor"),
    IN_PROGRESS("in_progress", "Devam Ediyor"),
    COMPLETED("completed", "Tamamlandı"),
    CANCELLED("cancelled", "İptal")
}

// ---------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _typeFilter = MutableStateFlow(TaskTypeFilter.ALL)
    val typeFilter: StateFlow<TaskTypeFilter> = _typeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    val statusFilter: StateFlow<TaskStatusFilter> = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** taskId -> istek uçuşta mı? Birden fazla görev aynı anda güncellenebilir. */
    private val _loadingItems = MutableStateFlow<Set<String>>(emptySet())
    val loadingItems: StateFlow<Set<String>> = _loadingItems.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            runCatching { repository.getAllTasks() }
                .onSuccess { _uiState.value = TaskUiState.Success(it) }
                .onFailure { _uiState.value = TaskUiState.Error(it.message ?: "Görevler yüklenemedi.") }
        }
    }

    fun setTypeFilter(filter: TaskTypeFilter) {
        _typeFilter.value = filter
    }

    fun setStatusFilter(filter: TaskStatusFilter) {
        _statusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Filtreler client-side uygulanır (tüm görevler önbellekte). */
    fun filteredTasks(all: List<TaskItemDto>): List<TaskItemDto> {
        val type = _typeFilter.value.apiValue
        val status = _statusFilter.value.apiValue
        val query = _searchQuery.value.trim()
        return all
            .filter { type == null || it.type == type }
            .filter { status == null || it.status == status }
            .filter {
                query.isEmpty() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.courseName?.contains(query, ignoreCase = true) == true
            }
            .sortedBy { it.startAt }
    }

    // ---------------------------------------------------------------
    // Checklist operations
    // ---------------------------------------------------------------

    fun toggleChecklistItem(taskId: String, itemId: String, currentDone: Boolean) {
        viewModelScope.launch {
            markItemLoading(itemId)
            runCatching { repository.updateChecklistItem(itemId, isDone = !currentDone) }
                .onSuccess { updated ->
                    updateChecklistItemInState(taskId, updated.id, isDone = updated.isDone)
                }
                .onFailure {
                    // Sessizce başarısız ol — kullanıcı tekrar deneyebilir.
                }
            unmarkItemLoading(itemId)
        }
    }

    fun addChecklistItem(taskId: String, title: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.addChecklistItem(taskId, title) }
                .onSuccess { newItem ->
                    appendChecklistItemInState(taskId, newItem)
                    onDone()
                }
                .onFailure {
                    // no-op: görev listesi kart üzerinde basit hata göstermiyoruz
                }
        }
    }

    fun deleteChecklistItem(taskId: String, itemId: String) {
        viewModelScope.launch {
            markItemLoading(itemId)
            runCatching { repository.deleteChecklistItem(itemId) }
                .onSuccess {
                    removeChecklistItemInState(taskId, itemId)
                }
                .onFailure { }
            unmarkItemLoading(itemId)
        }
    }

    // ---------------------------------------------------------------
    // In-memory state mutation helpers
    // ---------------------------------------------------------------

    private fun updateChecklistItemInState(taskId: String, itemId: String, isDone: Boolean) {
        val current = _uiState.value as? TaskUiState.Success ?: return
        _uiState.value = TaskUiState.Success(
            current.tasks.map { task ->
                if (task.id != taskId) task
                else {
                    val newChecklist = task.checklist.map { item ->
                        if (item.id == itemId) item.copy(isDone = isDone) else item
                    }
                    task.copy(
                        checklist = newChecklist,
                        checklistDone = newChecklist.count { it.isDone }
                    )
                }
            }
        )
    }

    private fun appendChecklistItemInState(taskId: String, newItem: com.unitrack.app.data.dto.ChecklistItemDto) {
        val current = _uiState.value as? TaskUiState.Success ?: return
        _uiState.value = TaskUiState.Success(
            current.tasks.map { task ->
                if (task.id != taskId) task
                else {
                    val newChecklist = task.checklist + newItem
                    task.copy(
                        checklist = newChecklist,
                        checklistTotal = newChecklist.size,
                        checklistDone = newChecklist.count { it.isDone }
                    )
                }
            }
        )
    }

    private fun removeChecklistItemInState(taskId: String, itemId: String) {
        val current = _uiState.value as? TaskUiState.Success ?: return
        _uiState.value = TaskUiState.Success(
            current.tasks.map { task ->
                if (task.id != taskId) task
                else {
                    val newChecklist = task.checklist.filter { it.id != itemId }
                    task.copy(
                        checklist = newChecklist,
                        checklistTotal = newChecklist.size,
                        checklistDone = newChecklist.count { it.isDone }
                    )
                }
            }
        )
    }

    private fun markItemLoading(itemId: String) {
        _loadingItems.value = _loadingItems.value + itemId
    }

    private fun unmarkItemLoading(itemId: String) {
        _loadingItems.value = _loadingItems.value - itemId
    }
}
