package com.unitrack.app.ui.calendar
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.theme.Spacing
import java.time.LocalTime

/**
 * "Dersleri takvime yerleştirme": haftalık ders programı (course_schedule)
 * için basit bir CRUD ekranı. Buradaki kayıtlar, Ay/Hafta görünümünde her
 * hafta otomatik tekrar eden "ders" satırları olarak görünür (bkz.
 * CalendarViewModel.scheduleOn).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    schedule: List<CourseScheduleDto>,
    courses: List<CourseDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (courseId: String, dayOfWeek: Int, startTime: LocalTime, endTime: LocalTime, location: String?) -> Unit,
    onUpdate: (id: String, courseId: String, dayOfWeek: Int, startTime: LocalTime, endTime: LocalTime, location: String?) -> Unit,
    onDelete: (id: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingEntry by remember { mutableStateOf<CourseScheduleDto?>(null) }
    var showForm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Haftalık Ders Programı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                PressableIconButton(onClick = {
                    editingEntry = null
                    showForm = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ders Programına Ekle")
                }
            }

            val sorted = remember(schedule) {
                schedule.sortedWith(compareBy({ (it.dayOfWeek + 6) % 7 }, { it.startTime }))
            }

            if (sorted.isEmpty()) {
                Text(
                    "Henüz haftalık ders programına ekleme yapılmadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.md)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(bottom = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(sorted) { entry ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(Spacing.cardPadding),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.courseName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${dayOfWeekLabel(entry.dayOfWeek)} · ${entry.startTime}–${entry.endTime}" +
                                            (entry.location?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PressableIconButton(onClick = {
                                    editingEntry = entry
                                    showForm = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Düzenle")
                                }
                                PressableIconButton(onClick = { onDelete(entry.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PremiumTextButton(onClick = onDismiss) { Text("Kapat") }
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    if (showForm) {
        ScheduleFormDialog(
            entry = editingEntry,
            courses = courses,
            isSaving = isSaving,
            onDismiss = { showForm = false },
            onSave = { courseId, dayOfWeek, startTime, endTime, location ->
                val current = editingEntry
                if (current == null) {
                    onCreate(courseId, dayOfWeek, startTime, endTime, location)
                } else {
                    onUpdate(current.id, courseId, dayOfWeek, startTime, endTime, location)
                }
                showForm = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFormDialog(
    entry: CourseScheduleDto?,
    courses: List<CourseDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (courseId: String, dayOfWeek: Int, startTime: LocalTime, endTime: LocalTime, location: String?) -> Unit
) {
    var selectedCourseId by remember { mutableStateOf(entry?.courseId ?: courses.firstOrNull()?.id) }
    var dayOfWeek by remember { mutableStateOf(entry?.dayOfWeek ?: java.time.LocalDate.now().dayOfWeek.toBackendDayOfWeek()) }
    var startTime by remember { mutableStateOf(entry?.startTime?.toLocalTimeOrNull() ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(entry?.endTime?.toLocalTimeOrNull() ?: LocalTime.of(10, 0)) }
    var location by remember { mutableStateOf(entry?.location ?: "") }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var courseMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Programa Ders Ekle" else "Ders Programını Düzenle") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseMenuExpanded,
                    onExpandedChange = { courseMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = courses.find { it.id == selectedCourseId }?.name ?: "Ders seç",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ders") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = courseMenuExpanded,
                        onDismissRequest = { courseMenuExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = { selectedCourseId = course.id; courseMenuExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = dayMenuExpanded,
                    onExpandedChange = { dayMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = dayOfWeekLabel(dayOfWeek),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gün") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dayMenuExpanded,
                        onDismissRequest = { dayMenuExpanded = false }
                    ) {
                        // Pazartesi..Pazar sırayla gösterilir (backend 0=Pazar..6=Cumartesi).
                        listOf(1, 2, 3, 4, 5, 6, 0).forEach { day ->
                            DropdownMenuItem(
                                text = { Text(dayOfWeekLabel(day)) },
                                onClick = { dayOfWeek = day; dayMenuExpanded = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = "%02d:%02d".format(startTime.hour, startTime.minute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Başlangıç") },
                        modifier = Modifier
                            .weight(1f)
                            .then(Modifier)
                    )
                    PressableIconButton(onClick = { showStartPicker = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Başlangıç saatini değiştir")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = "%02d:%02d".format(endTime.hour, endTime.minute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bitiş") },
                        modifier = Modifier.weight(1f)
                    )
                    PressableIconButton(onClick = { showEndPicker = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Bitiş saatini değiştir")
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Konum (opsiyonel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PremiumTextButton(
                enabled = selectedCourseId != null && !isSaving,
                onClick = {
                    selectedCourseId?.let { onSave(it, dayOfWeek, startTime, endTime, location.ifBlank { null }) }
                }
            ) { Text(if (isSaving) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = {
            PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )

    if (showStartPicker) {
        val state = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                PremiumTextButton(onClick = {
                    startTime = LocalTime.of(state.hour, state.minute)
                    showStartPicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { PremiumTextButton(onClick = { showStartPicker = false }) { Text("Vazgeç") } }
        )
    }

    if (showEndPicker) {
        val state = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                PremiumTextButton(onClick = {
                    endTime = LocalTime.of(state.hour, state.minute)
                    showEndPicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { PremiumTextButton(onClick = { showEndPicker = false }) { Text("Vazgeç") } }
        )
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = try {
    val parts = split(":")
    LocalTime.of(parts[0].toInt(), parts[1].toInt())
} catch (e: Exception) {
    null
}