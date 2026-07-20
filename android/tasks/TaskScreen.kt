package com.unitrack.app.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.TaskDto
import com.unitrack.app.ui.calendar.toLocalDateTimeOrNull
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.GlassFab
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Görev (ödev / proje / sunum) takibi. Backend: /api/v1/tasks (bkz. tasks
 * modülü). Her kart genişleyip checklist'i (alt görevleri) gösterebiliyor;
 * checklist öğeleri kendi endpoint'leri üzerinden bağımsız yönetiliyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var pendingDelete by remember { mutableStateOf<TaskDto?>(null) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            GlassFab(onClick = {
                editingTask = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Yeni Görev")
            }
        },
        snackbarHost = {
            errorMessage?.let {
                Snackbar(action = {
                    PremiumTextButton(onClick = { viewModel.clearError() }) { Text("Kapat") }
                }) { Text(it) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = filter == TaskFilter.ALL,
                        onClick = { viewModel.setFilter(TaskFilter.ALL) },
                        label = { Text("Tümü") }
                    )
                    FilterChip(
                        selected = filter == TaskFilter.PENDING,
                        onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                        label = { Text("Bekleyen") }
                    )
                    FilterChip(
                        selected = filter == TaskFilter.COMPLETED,
                        onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                        label = { Text("Tamamlanan") }
                    )
                }

                when {
                    isLoading && tasks.isEmpty() -> ListSkeleton(itemCount = 6)
                    tasks.isEmpty() -> EmptyState(
                        message = "Henüz görev eklenmedi. Sağ alttaki + butonuyla ekleyebilirsin.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xxl)
                    )
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(tasks, key = { _, t -> t.id }) { index, task ->
                                StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                    TaskCard(
                                        task = task,
                                        expanded = expandedTaskId == task.id,
                                        onToggleExpand = {
                                            expandedTaskId = if (expandedTaskId == task.id) null else task.id
                                        },
                                        onToggleStatus = { viewModel.toggleStatus(task.id) },
                                        onEdit = { editingTask = task; showEditor = true },
                                        onDelete = { pendingDelete = task },
                                        onAddChecklistItem = { title ->
                                            viewModel.addChecklistItem(task.id, title)
                                        },
                                        onToggleChecklistItem = { itemId, isDone ->
                                            viewModel.toggleChecklistItem(task.id, itemId, isDone)
                                        },
                                        onRemoveChecklistItem = { itemId ->
                                            viewModel.removeChecklistItem(task.id, itemId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorSheet(
            editingTask = editingTask,
            courses = courses,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { courseId, title, description, type, dueAt, priority, checklist ->
                val current = editingTask
                if (current == null) {
                    viewModel.createTask(
                        title = title,
                        courseId = courseId,
                        description = description,
                        type = type,
                        dueAt = dueAt,
                        priority = priority,
                        checklist = checklist
                    ) { success -> if (success) showEditor = false }
                } else {
                    viewModel.updateTask(
                        taskId = current.id,
                        title = title,
                        courseId = courseId,
                        description = description,
                        type = type,
                        dueAt = dueAt,
                        priority = priority
                    ) { success -> if (success) showEditor = false }
                }
            }
        )
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Görevi sil") },
            text = { Text("\"${task.title}\" görevini silmek istediğine emin misin?") },
            confirmButton = {
                PremiumTextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    pendingDelete = null
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                PremiumTextButton(onClick = { pendingDelete = null }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskDto,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String, Boolean) -> Unit,
    onRemoveChecklistItem: (String) -> Unit
) {
    val isCompleted = task.status == "completed"
    val priorityColor = when (task.priority) {
        "high" -> MaterialTheme.colorScheme.error
        "low" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))

                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Tamamlandı" else "Bekliyor",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                    )
                    val subtitle = buildString {
                        task.courseName?.let { append(it) }
                        task.dueAt?.toLocalDateTimeOrNull()?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it.format(DUE_DATE_FORMATTER))
                        }
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (task.checklistTotal > 0) {
                    Text(
                        "${task.checklistDone}/${task.checklistTotal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }

                Box {
                    PressableIconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Seçenekler")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }

                PressableIconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Daralt" else "Genişlet"
                    )
                }
            }

            if (task.checklistTotal > 0) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                LinearProgressIndicator(
                    progress = { task.checklistDone.toFloat() / task.checklistTotal.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Column(modifier = Modifier.padding(start = 44.dp)) {
                    if (!task.description.isNullOrBlank()) {
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    task.checklist.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { onToggleChecklistItem(item.id, it) }
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveChecklistItem(item.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Kaldır")
                            }
                        }
                    }

                    var newItemTitle by remember { mutableStateOf("") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            label = { Text("Alt görev ekle") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val trimmed = newItemTitle.trim()
                            if (trimmed.isNotEmpty()) {
                                onAddChecklistItem(trimmed)
                                newItemTitle = ""
                            }
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Ekle")
                        }
                    }

                    if (task.dueAt == null && task.checklistTotal == 0 && task.description.isNullOrBlank()) {
                        Text(
                            "Bu görev için ek detay yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private val DUE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
