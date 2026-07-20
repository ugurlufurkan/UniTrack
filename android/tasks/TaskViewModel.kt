package com.unitrack.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.TaskDto
import com.unitrack.app.data.repository.AcademicRepository
import com.unitrack.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ALL, PENDING, COMPLETED }

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val academicRepository: AcademicRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> = _tasks.asStateFlow()

    private val _courses = MutableStateFlow<List<CourseDto>>(emptyList())
    val courses: StateFlow<List<CourseDto>> = _courses.asStateFlow()

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            runCatching { academicRepository.getCourses() }
                .onSuccess { _courses.value = it }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val status = when (_filter.value) {
                TaskFilter.ALL -> null
                TaskFilter.PENDING -> "pending"
                TaskFilter.COMPLETED -> "completed"
            }
            runCatching { repository.getAll(status = status) }
                .onSuccess { _tasks.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Görevler alınamadı." }
            _isLoading.value = false
        }
    }

    fun createTask(
        title: String,
        courseId: String?,
        description: String?,
        type: String,
        dueAt: String?,
        priority: String,
        checklist: List<String>,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.create(
                    title = title,
                    courseId = courseId,
                    description = description,
                    type = type,
                    dueAt = dueAt,
                    priority = priority,
                    checklist = checklist
                )
            }
                .onSuccess { refresh(); onDone(true) }
                .onFailure { _errorMessage.value = it.message ?: "Görev oluşturulamadı."; onDone(false) }
            _isSaving.value = false
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        courseId: String?,
        description: String?,
        type: String,
        dueAt: String?,
        priority: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.update(
                    taskId = taskId,
                    courseId = courseId,
                    title = title,
                    description = description,
                    type = type,
                    dueAt = dueAt,
                    priority = priority
                )
            }
                .onSuccess { refresh(); onDone(true) }
                .onFailure { _errorMessage.value = it.message ?: "Görev güncellenemedi."; onDone(false) }
            _isSaving.value = false
        }
    }

    fun toggleStatus(taskId: String) {
        viewModelScope.launch {
            runCatching { repository.toggleStatus(taskId) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Güncellenemedi." }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching { repository.delete(taskId) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Silinemedi." }
        }
    }

    fun addChecklistItem(taskId: String, title: String) {
        viewModelScope.launch {
            runCatching { repository.addChecklistItem(taskId, title) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Eklenemedi." }
        }
    }

    fun toggleChecklistItem(taskId: String, itemId: String, isDone: Boolean) {
        viewModelScope.launch {
            runCatching { repository.updateChecklistItem(taskId, itemId, isDone = isDone) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Güncellenemedi." }
        }
    }

    fun removeChecklistItem(taskId: String, itemId: String) {
        viewModelScope.launch {
            runCatching { repository.removeChecklistItem(taskId, itemId) }
                .onSuccess { refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Silinemedi." }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
