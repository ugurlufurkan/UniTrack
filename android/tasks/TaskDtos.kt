package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// TASKS  (GET/POST/PUT/DELETE /api/v1/tasks)
// type: assignment | project | presentation | other
// priority: low | medium | high
// status: pending | completed
// ==========================

@Serializable
data class ChecklistItemDto(
    val id: String = "",
    val title: String = "",
    val isDone: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class TaskDto(
    val id: String = "",
    val courseId: String? = null,
    val courseName: String? = null,
    val title: String = "",
    val description: String? = null,
    val type: String = "assignment",
    val dueAt: String? = null,
    val priority: String = "medium",
    val status: String = "pending",
    val completedAt: String? = null,
    val checklist: List<ChecklistItemDto> = emptyList(),
    val checklistTotal: Int = 0,
    val checklistDone: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CreateTaskRequest(
    val courseId: String? = null,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val dueAt: String? = null,
    val priority: String? = null,
    val checklist: List<CreateChecklistItemRequest>? = null
)

@Serializable
data class UpdateTaskRequest(
    val courseId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val dueAt: String? = null,
    val priority: String? = null,
    val status: String? = null
)

@Serializable
data class CreateChecklistItemRequest(
    val title: String
)

@Serializable
data class UpdateChecklistItemRequest(
    val title: String? = null,
    val isDone: Boolean? = null,
    val sortOrder: Int? = null
)
