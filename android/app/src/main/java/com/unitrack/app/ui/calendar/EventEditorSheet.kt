package com.unitrack.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.data.dto.EventNotificationDto
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Etkinlik oluşturma / düzenleme formu. `editingEvent == null` ise "yeni
 * etkinlik", dolu ise "düzenle" modunda açılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    editingEvent: EventDto?,
    courses: List<CourseDto>,
    defaultDate: LocalDate,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        courseId: String?,
        title: String,
        description: String?,
        type: String,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        location: String?,
        priority: String,
        status: String,
        color: String,
        recurrence: String,
        notificationsEnabled: Boolean,
        notifications: List<EventNotificationDto>
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(editingEvent?.title ?: "") }
    var description by remember { mutableStateOf(editingEvent?.description ?: "") }
    var type by remember {
        mutableStateOf(EventTypeUi.fromApiValue(editingEvent?.type ?: "lesson"))
    }
    var selectedCourseId by remember { mutableStateOf(editingEvent?.courseId) }
    var location by remember { mutableStateOf(editingEvent?.location ?: "") }
    var priority by remember {
        mutableStateOf(EventPriorityUi.fromApiValue(editingEvent?.priority ?: "medium"))
    }
    var status by remember {
        mutableStateOf(EventStatusUi.fromApiValue(editingEvent?.status ?: "pending"))
    }
    var recurrence by remember {
        mutableStateOf(EventRecurrenceUi.fromApiValue(editingEvent?.recurrence ?: "none"))
    }
    var color by remember { mutableStateOf(editingEvent?.color ?: EventColorPalette.first()) }
    var notificationsEnabled by remember {
        mutableStateOf(editingEvent?.notificationsEnabled ?: true)
    }
    var notifications by remember {
        mutableStateOf(editingEvent?.notifications ?: emptyList())
    }

    val initialStart = editingEvent?.startAtLocalDateTimeOrNull()
        ?: LocalDateTime.of(defaultDate, LocalTime.of(9, 0))
    var startDateTime by remember { mutableStateOf(initialStart) }

    val initialEnd = editingEvent?.endAtLocalDateTimeOrNull()
    var hasEndDate by remember { mutableStateOf(initialEnd != null) }
    var endDateTime by remember { mutableStateOf(initialEnd ?: initialStart.plusHours(1)) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (editingEvent == null) "Yeni Etkinlik" else "Etkinliği Düzenle",
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

            EnumDropdown(
                label = "Tür",
                options = EventTypeUi.entries.toList(),
                selected = type,
                optionLabel = { it.label },
                onSelected = { type = it }
            )

            CourseDropdown(
                courses = courses,
                selectedCourseId = selectedCourseId,
                onSelected = { selectedCourseId = it }
            )

            // Başlangıç tarihi/saati
            Text("Başlangıç", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = startDateTime.toLocalDate().toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tarih") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartDatePicker = true }
                )
                OutlinedTextField(
                    value = "%02d:%02d".format(startDateTime.hour, startDateTime.minute),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Saat") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartTimePicker = true }
                )
            }

            // Bitiş tarihi/saati (opsiyonel)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                Text("Bitiş tarihi ekle (opsiyonel)")
            }
            if (hasEndDate) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = endDateTime.toLocalDate().toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bitiş tarihi") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true }
                    )
                    OutlinedTextField(
                        value = "%02d:%02d".format(endDateTime.hour, endDateTime.minute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Saat") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndTimePicker = true }
                    )
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Konum") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            EnumDropdown(
                label = "Öncelik",
                options = EventPriorityUi.entries.toList(),
                selected = priority,
                optionLabel = { it.label },
                onSelected = { priority = it }
            )

            EnumDropdown(
                label = "Durum",
                options = EventStatusUi.entries.toList(),
                selected = status,
                optionLabel = { it.label },
                onSelected = { status = it }
            )

            EnumDropdown(
                label = "Tekrarlama",
                options = EventRecurrenceUi.entries.toList(),
                selected = recurrence,
                optionLabel = { it.label },
                onSelected = { recurrence = it }
            )

            Text("Renk", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(EventColorPalette) { hex ->
                    ColorSwatch(
                        hex = hex,
                        selected = hex == color,
                        onClick = { color = hex }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bildirimler açık", style = MaterialTheme.typography.labelLarge)
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            if (notificationsEnabled) {
                NotificationsEditor(
                    notifications = notifications,
                    onChange = { notifications = it }
                )
            }

            if (editingEvent != null && onDelete != null) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Etkinliği Sil", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                Spacer(Modifier.width(Spacing.sm))
                PremiumTextButton(
                    onClick = {
                        onSave(
                            selectedCourseId,
                            title,
                            description.ifBlank { null },
                            type.apiValue,
                            startDateTime,
                            if (hasEndDate) endDateTime else null,
                            location.ifBlank { null },
                            priority.apiValue,
                            status.apiValue,
                            color,
                            recurrence.apiValue,
                            notificationsEnabled,
                            notifications
                        )
                    },
                    enabled = title.isNotBlank() && !isSaving
                ) { Text(if (isSaving) "Kaydediliyor…" else "Kaydet") }
            }

            Spacer(Modifier.height(Spacing.md))
        }
    }

    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDateTime.toLocalDate().toUtcEpochMillis()
        )
        AlertDialog(
            onDismissRequest = { showStartDatePicker = false },
            text = { DatePicker(state = state) },
            confirmButton = {
                PremiumTextButton(onClick = {
                    state.selectedDateMillis?.toUtcLocalDate()?.let {
                        startDateTime = LocalDateTime.of(it, startDateTime.toLocalTime())
                    }
                    showStartDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                PremiumTextButton(onClick = { showStartDatePicker = false }) { Text("Vazgeç") }
            }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialogHost(
            initial = startDateTime.toLocalTime(),
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startDateTime = LocalDateTime.of(startDateTime.toLocalDate(), it)
                showStartTimePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endDateTime.toLocalDate().toUtcEpochMillis()
        )
        AlertDialog(
            onDismissRequest = { showEndDatePicker = false },
            text = { DatePicker(state = state) },
            confirmButton = {
                PremiumTextButton(onClick = {
                    state.selectedDateMillis?.toUtcLocalDate()?.let {
                        endDateTime = LocalDateTime.of(it, endDateTime.toLocalTime())
                    }
                    showEndDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                PremiumTextButton(onClick = { showEndDatePicker = false }) { Text("Vazgeç") }
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogHost(
            initial = endDateTime.toLocalTime(),
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endDateTime = LocalDateTime.of(endDateTime.toLocalDate(), it)
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogHost(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            PremiumTextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Tamam")
            }
        },
        dismissButton = { PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDropdown(
    courses: List<CourseDto>,
    selectedCourseId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = courses.find { it.id == selectedCourseId }?.name ?: "Ders yok (bağımsız etkinlik)"

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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Ders yok (bağımsız etkinlik)") },
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

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseEventColor(hex))
                .then(
                    if (selected) Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ) else Modifier
                )
                .clickable(onClick = onClick)
        )
    }
}

/**
 * Kullanıcının istediği kadar özel bildirim ekleyip çıkarabildiği liste
 * (gün / saat / dakika önce). Her satır bağımsız bir EventNotificationDto.
 */
@Composable
private fun NotificationsEditor(
    notifications: List<EventNotificationDto>,
    onChange: (List<EventNotificationDto>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Özel Bildirimler", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = {
                onChange(notifications + EventNotificationDto(daysBefore = 0, hoursBefore = 1, minutesBefore = 0))
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Bildirim Ekle")
            }
        }

        notifications.forEachIndexed { index, notification ->
            NotificationRow(
                notification = notification,
                onChange = { updated ->
                    onChange(notifications.toMutableList().also { it[index] = updated })
                },
                onRemove = {
                    onChange(notifications.toMutableList().also { it.removeAt(index) })
                }
            )
        }

        if (notifications.isEmpty()) {
            Text(
                "Henüz özel bildirim eklenmedi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: EventNotificationDto,
    onChange: (EventNotificationDto) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        NumberField(
            value = notification.daysBefore,
            suffix = "gün",
            modifier = Modifier.weight(1f),
            onValueChange = { onChange(notification.copy(daysBefore = it)) }
        )
        NumberField(
            value = notification.hoursBefore,
            suffix = "sa",
            modifier = Modifier.weight(1f),
            onValueChange = { onChange(notification.copy(hoursBefore = it)) }
        )
        NumberField(
            value = notification.minutesBefore,
            suffix = "dk",
            modifier = Modifier.weight(1f),
            onValueChange = { onChange(notification.copy(minutesBefore = it)) }
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Kaldır")
        }
    }
}

@Composable
private fun NumberField(
    value: Int,
    suffix: String,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() }
            onValueChange(cleaned.toIntOrNull()?.coerceIn(0, 999) ?: 0)
        },
        label = { Text(suffix) },
        singleLine = true,
        modifier = modifier
    )
}

private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()