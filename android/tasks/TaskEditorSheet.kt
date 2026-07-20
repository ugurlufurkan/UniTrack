package com.unitrack.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.TaskDto
import com.unitrack.app.ui.calendar.toIsoUtc
import com.unitrack.app.ui.calendar.toLocalDateTimeOrNull
import com.unitrack.app.ui.components.PremiumButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

enum class TaskTypeUi(val apiValue: String, val label: String) {
    ASSIGNMENT("assignment", "Ödev"),
    PROJECT("project", "Proje"),
    PRESENTATION("presentation", "Sunum"),
    OTHER("other", "Diğer");

    companion object {
        fun fromApiValue(value: String): TaskTypeUi =
            entries.find { it.apiValue == value } ?: ASSIGNMENT
    }
}

enum class TaskPriorityUi(val apiValue: String, val label: String) {
    LOW("low", "Düşük"),
    MEDIUM("medium", "Orta"),
    HIGH("high", "Yüksek");

    companion object {
        fun fromApiValue(value: String): TaskPriorityUi =
            entries.find { it.apiValue == value } ?: MEDIUM
    }
}

/**
 * Görev oluşturma / düzenleme formu. `editingTask == null` ise "yeni görev",
 * dolu ise "düzenle" modunda açılır. Checklist öğeleri sadece OLUŞTURMA
 * sırasında burada eklenir (backend create isteği ilk checklist'i kabul
 * ediyor); mevcut bir görevin checklist'i ana listedeki genişleyen karttan
 * yönetilir, çünkü her öğe kendi endpoint'i üzerinden ayrı yaşıyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    editingTask: TaskDto?,
    courses: List<CourseDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        courseId: String?,
        title: String,
        description: String?,
        type: String,
        dueAt: String?,
        priority: String,
        checklist: List<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var description by remember { mutableStateOf(editingTask?.description ?: "") }
    var type by remember { mutableStateOf(TaskTypeUi.fromApiValue(editingTask?.type ?: "assignment")) }
    var priority by remember { mutableStateOf(TaskPriorityUi.fromApiValue(editingTask?.priority ?: "medium")) }
    var selectedCourseId by remember { mutableStateOf(editingTask?.courseId) }

    val initialDueAt = editingTask?.dueAt?.toLocalDateTimeOrNull()
    var hasDueDate by remember { mutableStateOf(initialDueAt != null) }
    var dueDateTime by remember {
        mutableStateOf(initialDueAt ?: LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Sadece yeni görev oluştururken kullanılan başlangıç checklist listesi.
    var checklistDrafts by remember {
        mutableStateOf(editingTask?.checklist?.map { it.title } ?: emptyList())
    }
    var newChecklistItem by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (editingTask == null) "Yeni Görev" else "Görevi Düzenle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            TaskEnumDropdown(
                label = "Tür",
                options = TaskTypeUi.entries.toList(),
                selected = type,
                optionLabel = { it.label },
                onSelected = { type = it }
            )

            TaskEnumDropdown(
                label = "Öncelik",
                options = TaskPriorityUi.entries.toList(),
                selected = priority,
                optionLabel = { it.label },
                onSelected = { priority = it }
            )

            TaskCourseDropdown(
                courses = courses,
                selectedCourseId = selectedCourseId,
                onSelected = { selectedCourseId = it }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = hasDueDate, onCheckedChange = { hasDueDate = it })
                Text("Son teslim tarihi ekle")
            }

            if (hasDueDate) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dueDateTime.toLocalDate().toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tarih") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                    )
                    OutlinedTextField(
                        value = "%02d:%02d".format(dueDateTime.hour, dueDateTime.minute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Saat") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker = true }
                    )
                }
            }

            if (editingTask == null) {
                Text("Alt Görevler (Checklist)", style = MaterialTheme.typography.labelLarge)

                checklistDrafts.forEachIndexed { index, itemTitle ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(itemTitle, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            checklistDrafts = checklistDrafts.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Kaldır")
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newChecklistItem,
                        onValueChange = { newChecklistItem = it },
                        label = { Text("Yeni öğe") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    IconButton(onClick = {
                        val trimmed = newChecklistItem.trim()
                        if (trimmed.isNotEmpty()) {
                            checklistDrafts = checklistDrafts + trimmed
                            newChecklistItem = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Ekle")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PremiumTextButton(onClick = onDismiss, enabled = !isSaving) { Text("Vazgeç") }
                Spacer(modifier = Modifier.width(Spacing.sm))
                PremiumButton(
                    enabled = title.isNotBlank() && !isSaving,
                    onClick = {
                        onSave(
                            selectedCourseId,
                            title.trim(),
                            description.trim().ifBlank { null },
                            type.apiValue,
                            if (hasDueDate) dueDateTime.toIsoUtc() else null,
                            priority.apiValue,
                            checklistDrafts
                        )
                    }
                ) {
                    Text(if (editingTask == null) "Oluştur" else "Kaydet")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateTime.toLocalDate().toUtcEpochMillis()
        )
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            text = { DatePicker(state = datePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dueDateTime = dueDateTime.toLocalTime().let { time ->
                            LocalDateTime.of(it.toUtcLocalDate(), time)
                        }
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { PremiumTextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") } }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dueDateTime.hour,
            initialMinute = dueDateTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    dueDateTime = dueDateTime.toLocalDate().let { date ->
                        LocalDateTime.of(date, LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }
                    showTimePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { PremiumTextButton(onClick = { showTimePicker = false }) { Text("Vazgeç") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> TaskEnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCourseDropdown(
    courses: List<CourseDto>,
    selectedCourseId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = courses.find { it.id == selectedCourseId }?.name ?: "Ders yok (bağımsız görev)"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ders") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Ders yok (bağımsız görev)") },
                onClick = { onSelected(null); expanded = false }
            )
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text(course.name) },
                    onClick = { onSelected(course.id); expanded = false }
                )
            }
        }
    }
}

private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
