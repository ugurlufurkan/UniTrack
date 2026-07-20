package com.unitrack.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.GlassFab
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showEditor by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventDto?>(null) }
    var newEventDate by remember { mutableStateOf(LocalDate.now()) }
    var showScheduleEditor by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            GlassFab(onClick = {
                haptic.click()
                editingEvent = null
                newEventDate = uiState.focusedDate
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Yeni Etkinlik")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(Modifier.height(Spacing.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ViewModeSelector(
                        selected = uiState.viewMode,
                        onSelect = { viewModel.setViewMode(it) }
                    )
                }
                PressableIconButton(onClick = {
                    haptic.click()
                    showScheduleEditor = true
                }) {
                    Icon(Icons.Filled.EditCalendar, contentDescription = "Haftalık Ders Programı")
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            when {
                uiState.isLoading -> ListSkeleton()
                uiState.errorMessage != null && uiState.events.isEmpty() -> ErrorState(
                    message = uiState.errorMessage ?: "Takvim yüklenemedi.",
                    onRetry = { viewModel.refresh() }
                )
                else -> when (uiState.viewMode) {
                    CalendarViewMode.MONTH -> MonthView(
                        viewModel = viewModel,
                        focusedDate = uiState.focusedDate,
                        onDayClick = { date ->
                            newEventDate = date
                            viewModel.setFocusedDate(date)
                        },
                        onEventClick = { editingEvent = it; showEditor = true }
                    )
                    CalendarViewMode.WEEK -> WeekView(
                        viewModel = viewModel,
                        focusedDate = uiState.focusedDate,
                        onEventClick = { editingEvent = it; showEditor = true }
                    )
                    CalendarViewMode.LIST -> ListModeView(
                        events = uiState.events,
                        onEventClick = { editingEvent = it; showEditor = true }
                    )
                    CalendarViewMode.UPCOMING -> UpcomingView(
                        events = uiState.events,
                        onEventClick = { editingEvent = it; showEditor = true }
                    )
                }
            }
        }
    }

    if (showEditor) {
        EventEditorSheet(
            editingEvent = editingEvent,
            courses = courses,
            defaultDate = newEventDate,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { courseId, title, description, type, startAt, endAt, location,
                priority, status, color, recurrence, notificationsEnabled, notifications ->
                val current = editingEvent
                if (current == null) {
                    viewModel.createEvent(
                        courseId, title, description, type, startAt, endAt, location,
                        priority, status, color, recurrence, notificationsEnabled, notifications
                    ) { success -> if (success) showEditor = false }
                } else {
                    viewModel.updateEvent(
                        current.id, courseId, title, description, type, startAt, endAt, location,
                        priority, status, color, recurrence, notificationsEnabled, notifications
                    ) { success -> if (success) showEditor = false }
                }
            },
            onDelete = editingEvent?.let { event ->
                {
                    viewModel.deleteEvent(event.id)
                    showEditor = false
                }
            }
        )
    }

    if (showScheduleEditor) {
        ScheduleEditorSheet(
            schedule = uiState.schedule,
            courses = courses,
            isSaving = isSaving,
            onDismiss = { showScheduleEditor = false },
            onCreate = { courseId, dayOfWeek, startTime, endTime, location ->
                viewModel.createSchedule(courseId, dayOfWeek, startTime, endTime, location)
            },
            onUpdate = { id, courseId, dayOfWeek, startTime, endTime, location ->
                viewModel.updateSchedule(id, courseId, dayOfWeek, startTime, endTime, location)
            },
            onDelete = { id -> viewModel.deleteSchedule(id) }
        )
    }
}

@Composable
private fun ViewModeSelector(
    selected: CalendarViewMode,
    onSelect: (CalendarViewMode) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(CalendarViewMode.entries.toList()) { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun PeriodHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressableIconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Önceki")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(Spacing.xs))
            PressableIconButton(onClick = onToday, size = 32.dp) {
                Icon(Icons.Filled.Today, contentDescription = "Bugün", modifier = Modifier.size(18.dp))
            }
        }
        PressableIconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Sonraki")
        }
    }
}

// =====================================================================
// AY GÖRÜNÜMÜ
// =====================================================================

@Composable
private fun MonthView(
    viewModel: CalendarViewModel,
    focusedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    onEventClick: (EventDto) -> Unit
) {
    val yearMonth = YearMonth.from(focusedDate)
    val monthTitle = focusedDate.month.getDisplayName(TextStyle.FULL, Locale("tr"))
        .replaceFirstChar { it.uppercase() } + " " + focusedDate.year

    val firstOfMonth = yearMonth.atDay(1)
    // Pazartesi başlangıçlı hafta: baştaki boşluk sayısı.
    val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val trailingBlanks = (7 - totalCells % 7).let { if (it == 7) 0 else it }
    val cells: List<LocalDate?> = buildList {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..daysInMonth) add(yearMonth.atDay(day))
        repeat(trailingBlanks) { add(null) }
    }

    Column {
        PeriodHeader(
            title = monthTitle,
            onPrevious = { viewModel.goToPreviousPeriod() },
            onNext = { viewModel.goToNextPeriod() },
            onToday = { viewModel.goToToday() }
        )

        Spacer(Modifier.height(Spacing.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val weeks = cells.chunked(7)
        Column(modifier = Modifier.fillMaxWidth()) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            isSelected = date != null && date == focusedDate,
                            isToday = date != null && date == LocalDate.now(),
                            events = date?.let { viewModel.eventsOn(it) } ?: emptyList(),
                            hasLessons = date?.let { viewModel.scheduleOn(it).isNotEmpty() } ?: false,
                            onClick = { date?.let(onDayClick) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = "${focusedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale("tr")))} etkinlikleri",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.sm))

        val dayEvents = viewModel.eventsOn(focusedDate)
        val daySchedule = viewModel.scheduleOn(focusedDate)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(daySchedule) { schedule -> ScheduleListItem(schedule) }
            items(dayEvents) { event -> EventListItem(event = event, onClick = { onEventClick(event) }) }
            if (dayEvents.isEmpty() && daySchedule.isEmpty()) {
                item {
                    Text(
                        "Bu tarihte etkinlik yok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<EventDto>,
    hasLessons: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasLessons) {
                        Dot(color = MaterialTheme.colorScheme.tertiary)
                    }
                    events.take(3).forEach { event ->
                        Dot(color = parseEventColor(event.color))
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// =====================================================================
// HAFTA GÖRÜNÜMÜ
// =====================================================================

@Composable
private fun WeekView(
    viewModel: CalendarViewModel,
    focusedDate: LocalDate,
    onEventClick: (EventDto) -> Unit
) {
    val monday = focusedDate.minusDays((focusedDate.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }
    val title = "${weekDays.first().format(DateTimeFormatter.ofPattern("d MMM", Locale("tr")))} – " +
        weekDays.last().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr")))

    Column {
        PeriodHeader(
            title = title,
            onPrevious = { viewModel.goToPreviousPeriod() },
            onNext = { viewModel.goToNextPeriod() },
            onToday = { viewModel.goToToday() }
        )

        Spacer(Modifier.height(Spacing.sm))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items(weekDays) { date ->
                val events = viewModel.eventsOn(date)
                val schedule = viewModel.scheduleOn(date)
                if (events.isNotEmpty() || schedule.isNotEmpty() || date == LocalDate.now()) {
                    Column {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("tr")))
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (date == LocalDate.now()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            schedule.forEach { ScheduleListItem(it) }
                            events.forEach { EventListItem(event = it, onClick = { onEventClick(it) }) }
                            if (events.isEmpty() && schedule.isEmpty()) {
                                Text(
                                    "Etkinlik yok",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// LİSTE GÖRÜNÜMÜ — tüm etkinlikler kronolojik
// =====================================================================

@Composable
private fun ListModeView(
    events: List<EventDto>,
    onEventClick: (EventDto) -> Unit
) {
    val sorted = remember(events) { events.sortedBy { it.startAtLocalDateTimeOrNull() } }

    if (sorted.isEmpty()) {
        Text(
            "Henüz etkinlik eklenmedi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.lg)
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(sorted) { event -> EventListItem(event = event, showDate = true, onClick = { onEventClick(event) }) }
    }
}

// =====================================================================
// YAKLAŞAN ETKİNLİKLER
// =====================================================================

@Composable
private fun UpcomingView(
    events: List<EventDto>,
    onEventClick: (EventDto) -> Unit
) {
    val now = LocalDate.now()
    val upcoming = remember(events) {
        events
            .filter { it.status == "pending" || it.status == "in_progress" }
            .filter { (it.startAtLocalDateOrNull() ?: now).let { d -> !d.isBefore(now) } }
            .sortedBy { it.startAtLocalDateTimeOrNull() }
    }

    if (upcoming.isEmpty()) {
        Text(
            "Yaklaşan etkinlik yok.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.lg)
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(upcoming) { event -> EventListItem(event = event, showDate = true, onClick = { onEventClick(event) }) }
    }
}

// =====================================================================
// Ortak liste satırları
// =====================================================================

@Composable
private fun EventListItem(
    event: EventDto,
    showDate: Boolean = false,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseEventColor(event.color))
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeLabel = event.startAtLocalDateTimeOrNull()?.let {
                    val pattern = if (showDate) "d MMM, HH:mm" else "HH:mm"
                    it.format(DateTimeFormatter.ofPattern(pattern, Locale("tr")))
                } ?: ""
                val subtitle = listOfNotNull(
                    EventTypeUi.fromApiValue(event.type).label,
                    timeLabel.takeIf { it.isNotBlank() },
                    event.courseName
                ).joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!event.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            EventStatusBadge(status = event.status)
        }
    }
}

@Composable
private fun ScheduleListItem(schedule: CourseScheduleDto) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.courseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ders · ${schedule.startTime}–${schedule.endTime}" +
                        (schedule.location?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EventStatusBadge(status: String) {
    val ui = EventStatusUi.fromApiValue(status)
    val color = when (ui) {
        EventStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        EventStatusUi.CANCELLED -> MaterialTheme.colorScheme.error
        EventStatusUi.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        EventStatusUi.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
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
