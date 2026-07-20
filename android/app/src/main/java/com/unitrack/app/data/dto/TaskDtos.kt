package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// CHECKLIST ITEM
// ==========================
@Serializable
data class ChecklistItemDto(
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ==========================
// TASK ITEM  (GET /api/v1/tasks, GET /api/v1/tasks/{id})
// Her biri aslında assignment | project | presentation türünde bir takvim etkinliği.
// ==========================
@Serializable
data class TaskItemDto(
    val id: String = "",
    val userId: String = "",
    val courseId: String? = null,
    val courseName: String? = null,
    val title: String = "",
    val description: String? = null,
    // assignment | project | presentation
    val type: String = "assignment",
    val startAt: String = "",
    val endAt: String? = null,
    val location: String? = null,
    // low | medium | high
    val priority: String = "medium",
    // pending | in_progress | completed | cancelled
    val status: String = "pending",
    val color: String = "#6366F1",
    val recurrence: String = "none",
    val notificationsEnabled: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
    val checklist: List<ChecklistItemDto> = emptyList(),
    val checklistTotal: Int = 0,
    val checklistDone: Int = 0
)

// ==========================
// CHECKLIST REQUESTS
// ==========================
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

@Serializable
data class ReorderChecklistRequest(
    val itemIds: List<String>
)

// ==========================
// SIMPLE SUCCESS RESPONSE
// ==========================
@Serializable
data class TaskSimpleResponse(
    val success: Boolean = false
)
