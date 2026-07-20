package com.unitrack.app.ui.calendar
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unitrack.app.data.dto.CalendarSummaryDto
import com.unitrack.app.data.dto.CourseScheduleDto
import com.unitrack.app.data.dto.EventDto
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dashboard'daki "Bugünkü Dersler / Yaklaşan Sınavlar / Yaklaşan Teslimler /
 * Geciken Ödevler" özet kartları. Tek veri kaynağı: GET /calendar/summary
 * (bkz. CalendarRepository.getCalendarSummary, DashboardViewModel.calendarSummary).
 *
 * Takvim modülü hiç kullanılmamışsa (tüm listeler boş) hiçbir şey çizmiyor —
 * dashboard'da boş kart yığını görünmesin diye.
 */
@Composable
fun DashboardCalendarSummarySection(
    summary: CalendarSummaryDto,
    onClick: () -> Unit
) {
    val hasAnything = summary.todayLessons.isNotEmpty() ||
        summary.todayClasses.isNotEmpty() ||
        summary.upcomingExams.isNotEmpty() ||
        summary.upcomingDeadlines.isNotEmpty() ||
        summary.overdueAssignments.isNotEmpty()

    if (!hasAnything) return

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {

        if (summary.overdueAssignments.isNotEmpty()) {
            SummaryCard(
                title = "Geciken Ödevler",
                icon = Icons.Filled.Warning,
                accentColor = MaterialTheme.colorScheme.error,
                onClick = onClick
            ) {
                summary.overdueAssignments.take(3).forEach { CompactEventRow(it) }
            }
        }

        if (summary.todayClasses.isNotEmpty() || summary.todayLessons.isNotEmpty()) {
            SummaryCard(
                title = "Bugünkü Dersler",
                icon = null,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = onClick
            ) {
                summary.todayClasses.take(4).forEach { CompactScheduleRow(it) }
                summary.todayLessons.take(4).forEach { CompactEventRow(it) }
            }
        }

        if (summary.upcomingExams.isNotEmpty()) {
            SummaryCard(
                title = "Yaklaşan Sınavlar",
                icon = null,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onClick
            ) {
                summary.upcomingExams.take(3).forEach { CompactEventRow(it, showDate = true) }
            }
        }

        if (summary.upcomingDeadlines.isNotEmpty()) {
            SummaryCard(
                title = "Yaklaşan Teslimler",
                icon = null,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onClick
            ) {
                summary.upcomingDeadlines.take(3).forEach { CompactEventRow(it, showDate = true) }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            content()
        }
    }
}

@Composable
private fun CompactEventRow(event: EventDto, showDate: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val timeLabel = event.startAtLocalDateTimeOrNull()?.let {
            val pattern = if (showDate) "d MMM HH:mm" else "HH:mm"
            it.format(DateTimeFormatter.ofPattern(pattern, Locale("tr")))
        }
        if (timeLabel != null) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactScheduleRow(schedule: CourseScheduleDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = schedule.courseName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${schedule.startTime}–${schedule.endTime}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
