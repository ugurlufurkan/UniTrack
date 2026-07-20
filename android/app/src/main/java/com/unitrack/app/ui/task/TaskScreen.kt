package com.unitrack.app.ui.task

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.ChecklistItemDto
import com.unitrack.app.data.dto.TaskItemDto
import com.unitrack.app.ui.calendar.EventStatusUi
import com.unitrack.app.ui.calendar.parseEventColor
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.SearchField
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------
// Public entry point — nav host'a drop-in composable
// ---------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(onBack: () -> Unit = {}) {
    val viewModel: TaskViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val loadingItems by viewModel.loadingItems.collectAsState()
    val haptic = LocalHapticFeedback.current

    var expandedTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(Modifier.height(Spacing.sm))

            SearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Görev veya ders ara...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.sm))

            // --- Tür filtre çipleri ---
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(TaskTypeFilter.entries.toList()) { filter ->
                    FilterChip(
                        selected = typeFilter == filter,
                        onClick = { viewModel.setTypeFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            // --- Durum filtre çipleri ---
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(TaskStatusFilter.entries.toList()) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            when (val state = uiState) {
                is TaskUiState.Loading -> ListSkeleton()

                is TaskUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )

                is TaskUiState.Success -> {
                    val filtered = viewModel.filteredTasks(state.tasks)

                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = {
                            haptic.click()
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (filtered.isEmpty()) {
                            EmptyState(
                                message = if (searchQuery.isNotBlank() ||
                                    typeFilter != TaskTypeFilter.ALL ||
                                    statusFilter != TaskStatusFilter.ALL
                                ) {
                                    "Bu filtrelerle eşleşen görev yok."
                                } else {
                                    "Henüz görev yok — Takvim'den ödev/proje/sunum ekleyebilirsin."
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                contentPadding = PaddingValues(bottom = Spacing.lg)
                            ) {
                                items(filtered, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        isExpanded = expandedTaskId == task.id,
                                        loadingItems = loadingItems,
                                        onToggleExpand = {
                                            haptic.click()
                                            expandedTaskId =
                                                if (expandedTaskId == task.id) null else task.id
                                        },
                                        onToggleChecklist = { itemId, isDone ->
                                            viewModel.toggleChecklistItem(task.id, itemId, isDone)
                                        },
                                        onAddChecklist = { title ->
                                            viewModel.addChecklistItem(task.id, title)
                                        },
                                        onDeleteChecklist = { itemId ->
                                            viewModel.deleteChecklistItem(task.id, itemId)
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
}

// ---------------------------------------------------------------
// Task card — collapsed header + optional expanded checklist
// ---------------------------------------------------------------

@Composable
private fun TaskCard(
    task: TaskItemDto,
    isExpanded: Boolean,
    loadingItems: Set<String>,
    onToggleExpand: () -> Unit,
    onToggleChecklist: (itemId: String, currentIsDone: Boolean) -> Unit,
    onAddChecklist: (title: String) -> Unit,
    onDeleteChecklist: (itemId: String) -> Unit
) {
    val accentColor = parseEventColor(task.color)
    val isOverdue = task.isOverdue()
    val overdueColor = MaterialTheme.colorScheme.error

    GlassCard(
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {

            // --- Başlık satırı ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    val dueDateLabel = task.startAt.toLocalDateOrNull()
                        ?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr")))
                        ?: ""
                    val subtitle = listOfNotNull(
                        task.courseName,
                        dueDateLabel.takeIf { it.isNotBlank() }
                            ?.let { if (isOverdue) "⚠ $it" else it }
                    ).joinToString(" · ")

                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) overdueColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(Spacing.sm))

                Column(horizontalAlignment = Alignment.End) {
                    TaskStatusChip(status = task.status)
                    Spacer(Modifier.height(Spacing.xs))
                    PriorityChip(priority = task.priority)
                }
            }

            // --- Checklist ilerleme çubuğu ---
            if (task.checklistTotal > 0) {
                Spacer(Modifier.height(Spacing.sm))
                val progress =
                    task.checklistDone.toFloat() / task.checklistTotal.toFloat()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        strokeCap = StrokeCap.Round,
                        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "${task.checklistDone}/${task.checklistTotal} tamamlandı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Genişletilmiş checklist ---
            if (isExpanded) {
                Spacer(Modifier.height(Spacing.sm))
                ChecklistSection(
                    task = task,
                    loadingItems = loadingItems,
                    onToggle = onToggleChecklist,
                    onAdd = onAddChecklist,
                    onDelete = onDeleteChecklist
                )
            }
        }
    }
}

// ---------------------------------------------------------------
// Checklist bölümü (genişletilmiş)
// ---------------------------------------------------------------

@Composable
private fun ChecklistSection(
    task: TaskItemDto,
    loadingItems: Set<String>,
    onToggle: (itemId: String, currentIsDone: Boolean) -> Unit,
    onAdd: (title: String) -> Unit,
    onDelete: (itemId: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var newItemTitle by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        task.checklist
            .sortedBy { it.sortOrder }
            .forEach { item ->
                ChecklistRow(
                    item = item,
                    isLoading = item.id in loadingItems,
                    onToggle = { onToggle(item.id, item.isDone) },
                    onDelete = { onDelete(item.id) }
                )
            }

        Spacer(Modifier.height(Spacing.xs))

        // Yeni alt görev ekleme
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newItemTitle,
                onValueChange = { newItemTitle = it },
                placeholder = {
                    Text(
                        "+ Alt görev ekle",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val title = newItemTitle.trim()
                        if (title.isNotBlank()) {
                            onAdd(title)
                            newItemTitle = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = RoundedCornerShape(Spacing.sm)
            )
            Spacer(Modifier.width(Spacing.xs))
            IconButton(
                onClick = {
                    val title = newItemTitle.trim()
                    if (title.isNotBlank()) {
                        onAdd(title)
                        newItemTitle = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = newItemTitle.isNotBlank()
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Alt görev ekle",
                    tint = if (newItemTitle.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------
// Checklist satırı
// ---------------------------------------------------------------

@Composable
private fun ChecklistRow(
    item: ChecklistItemDto,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onToggle,
            enabled = !isLoading,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (item.isDone) Icons.Filled.CheckBox
                              else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (item.isDone) "Tamamlandı" else "Tamamlanmadı",
                tint = if (item.isDone) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.isDone) TextDecoration.LineThrough
                             else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = onDelete,
            enabled = !isLoading,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------
// Durum çipi
// ---------------------------------------------------------------

@Composable
private fun TaskStatusChip(status: String) {
    val ui = EventStatusUi.fromApiValue(status)
    val color = when (ui) {
        EventStatusUi.COMPLETED  -> MaterialTheme.colorScheme.tertiary
        EventStatusUi.CANCELLED  -> MaterialTheme.colorScheme.error
        EventStatusUi.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        EventStatusUi.PENDING    -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = ui.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ---------------------------------------------------------------
// Öncelik çipi
// ---------------------------------------------------------------

@Composable
private fun PriorityChip(priority: String) {
    val (label, color) = when (priority) {
        "high"   -> "Yüksek" to MaterialTheme.colorScheme.error
        "medium" -> "Orta"   to Color(0xFFFFC168)
        else     -> "Düşük"  to MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ---------------------------------------------------------------
// Tarih yardımcıları
// ---------------------------------------------------------------

private fun String.toLocalDateOrNull(): LocalDate? = try {
    Instant.parse(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
} catch (_: Exception) {
    null
}

private fun TaskItemDto.isOverdue(): Boolean {
    if (status == "completed" || status == "cancelled") return false
    val due = startAt.toLocalDateOrNull() ?: return false
    return due.isBefore(LocalDate.now())
}
