package com.unitrack.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.AttendanceCourseDetailDto
import com.unitrack.app.data.dto.AttendanceRecordDto
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PremiumButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAttendanceScreen(
    courseId: String,
    courseName: String,
    onBack: () -> Unit = {},
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val courseState by viewModel.courseState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var showSettingsEditor by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.loadCourse(courseId)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(courseName, fontWeight = FontWeight.Bold)
                        Text(
                            "Devamsızlık Detayı",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    PressableIconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    PressableIconButton(onClick = { showSettingsEditor = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Devamsızlık ayarlarını düzenle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            when (val state = courseState) {
                is CourseAttendanceUiState.Loading -> ListSkeleton()
                is CourseAttendanceUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.clearCourseError(courseId) }
                )
                is CourseAttendanceUiState.Success -> CourseDetailContent(
                    detail = state.detail,
                    isSaving = isSaving,
                    onMarkWeek = { weekNumber, attended, absent, excused, existingDate ->
                        viewModel.markWeekHours(
                            courseId, weekNumber, attended, absent, excused, existingDate
                        )
                    },
                    onDeleteRecord = { recordId ->
                        viewModel.deleteRecord(courseId, recordId)
                    }
                )
            }
        }
    }

    // Edit attendance settings dialog (total weeks + weekly hours + hour limit)
    if (showSettingsEditor) {
        val current = (courseState as? CourseAttendanceUiState.Success)?.detail
        EditAttendanceSettingsDialog(
            currentWeeks = current?.totalWeeks ?: 14,
            currentWeeklyHours = current?.weeklyHours ?: 3,
            currentLimitHours = current?.attendanceLimitHours ?: 0,
            onDismiss = { showSettingsEditor = false },
            onConfirm = { newWeeks, newWeeklyHours, newLimitHours ->
                viewModel.updateSettings(
                    courseId = courseId,
                    totalWeeks = newWeeks,
                    weeklyHours = newWeeklyHours,
                    attendanceLimitHours = newLimitHours
                ) { showSettingsEditor = false }
            }
        )
    }
}

@Composable
private fun CourseDetailContent(
    detail: AttendanceCourseDetailDto,
    isSaving: Boolean,
    onMarkWeek: (weekNumber: Int, attended: Int, absent: Int, excused: Int, existingDate: String?) -> Unit,
    onDeleteRecord: (recordId: String) -> Unit
) {
    val recordsByWeek = remember(detail.records) {
        detail.records.associateBy { it.weekNumber }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item {
            CourseStatusCard(detail)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Haftalık Devamsızlık (${detail.weeklyHours} sa/hafta)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        items((1..detail.totalWeeks).toList()) { week ->
            val record = recordsByWeek[week]
            WeekRow(
                weekNumber = week,
                weeklyHours = detail.weeklyHours,
                record = record,
                isSaving = isSaving,
                onMark = { attended, absent, excused ->
                    onMarkWeek(week, attended, absent, excused, record?.date)
                },
                onDelete = record?.let { r -> { onDeleteRecord(r.id) } }
            )
        }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

@Composable
private fun CourseStatusCard(detail: AttendanceCourseDetailDto) {
    val warningColor = MaterialTheme.colorScheme.error
    val safeColor = MaterialTheme.colorScheme.tertiary
    val accentColor = if (detail.isAtRisk) warningColor else safeColor
    val hasLimit = detail.attendanceLimitHours > 0

    GlassCard {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            // Risk badge
            if (detail.isAtRisk) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(warningColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = warningColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "⚠️ Devamsızlık riski — limit aşıldı!",
                        style = MaterialTheme.typography.labelSmall,
                        color = warningColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Devamsız saat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${detail.absentHours} sa",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = if (hasLimit) "Limit: ${detail.attendanceLimitHours} sa" else "Limit ayarlanmadı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Kalan hak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasLimit) "${detail.remainingAllowedHours} sa" else "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasLimit && (detail.remainingAllowedHours ?: 0) <= detail.weeklyHours) warningColor else safeColor
                    )
                    Text(
                        text = "toplam ${detail.totalCourseHours} sa ders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("Katıldı", detail.attendedHours, safeColor)
                MiniStat("Devamsız", detail.absentHours, warningColor)
                MiniStat("İzinli", detail.excusedHours, MaterialTheme.colorScheme.secondary)
                MiniStat("İşaretsiz hafta", detail.unmarkedWeeks, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Bir haftaya ait satır: dersin o haftaki toplam saatini (weeklyHours)
 * katıldı / katılmadı / izinli olarak saat bazında paylaştırır.
 * Örn. Mat1 haftada 4 saatse: 2 katıldı + 1 katılmadı + 1 izinli gibi.
 */
@Composable
private fun WeekRow(
    weekNumber: Int,
    weeklyHours: Int,
    record: AttendanceRecordDto?,
    isSaving: Boolean,
    onMark: (attended: Int, absent: Int, excused: Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    val safeColor = MaterialTheme.colorScheme.tertiary
    val warningColor = MaterialTheme.colorScheme.error
    val secondaryColor = MaterialTheme.colorScheme.secondary

    var attended by remember(record?.id, weekNumber) { mutableIntStateOf(record?.attendedHours ?: weeklyHours) }
    var absent by remember(record?.id, weekNumber) { mutableIntStateOf(record?.absentHours ?: 0) }
    var excused by remember(record?.id, weekNumber) { mutableIntStateOf(record?.excusedHours ?: 0) }

    val marked = record != null
    val dateLabel = record?.date?.let {
        runCatching {
            val instant = java.time.Instant.parse(it)
            val ld = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            ld.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr")))
        }.getOrNull()
    }

    // Girilen saatler weeklyHours'u geçmesin — fazlasını otomatik kırp
    fun clamp(newValue: Int, others: Int): Int =
        newValue.coerceIn(0, (weeklyHours - others).coerceAtLeast(0))

    GlassCard {
        Column(modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$weekNumber. Hafta",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (dateLabel != null) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            (if (marked) safeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                .copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (marked) "$attended/$weeklyHours sa katıldı" else "İşaretlenmedi",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (marked) safeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                HourStepper(
                    label = "Katıldı",
                    value = attended,
                    color = safeColor,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        attended = clamp(it, absent + excused)
                        onMark(attended, absent, excused)
                    }
                )
                HourStepper(
                    label = "Katılmadı",
                    value = absent,
                    color = warningColor,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        absent = clamp(it, attended + excused)
                        onMark(attended, absent, excused)
                    }
                )
                HourStepper(
                    label = "İzinli",
                    value = excused,
                    color = secondaryColor,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        excused = clamp(it, attended + absent)
                        onMark(attended, absent, excused)
                    }
                )
                if (record != null && onDelete != null) {
                    PressableIconButton(
                        onClick = if (isSaving) ({}) else onDelete,
                        size = 32.dp
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Kaydı sil",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Tek bir saat sayacı: -/+ butonlarıyla 0..weeklyHours arasında saat girilir. */
@Composable
private fun HourStepper(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    val bgColor = if (value > 0) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (value > 0) color else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            StepperButton(symbol = "–", enabled = enabled && value > 0, onClick = { onValueChange(value - 1) })
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.width(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            StepperButton(symbol = "+", enabled = enabled, onClick = { onValueChange(value + 1) })
        }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun EditAttendanceSettingsDialog(
    currentWeeks: Int,
    currentWeeklyHours: Int,
    currentLimitHours: Int,
    onDismiss: () -> Unit,
    onConfirm: (weeks: Int, weeklyHours: Int, limitHours: Int) -> Unit
) {
    var weeksSlider by remember { mutableFloatStateOf(currentWeeks.toFloat()) }
    var weeklyHoursSlider by remember { mutableFloatStateOf(currentWeeklyHours.toFloat()) }
    // 0 = henüz ayarlanmadı; kullanıcı dokununca makul bir başlangıç değeri göster
    var limitSlider by remember {
        mutableFloatStateOf(
            if (currentLimitHours > 0) currentLimitHours.toFloat()
            else (currentWeeks * currentWeeklyHours * 0.15f).coerceAtLeast(1f)
        )
    }
    val weeks = weeksSlider.toInt()
    val weeklyHours = weeklyHoursSlider.toInt()
    val limit = limitSlider.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Devamsızlık Ayarları") },
        text = {
            Column {
                Text(
                    text = "$weeks hafta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = weeksSlider,
                    onValueChange = { weeksSlider = it },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Dönemde dersin işleneceği toplam hafta sayısı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "Haftada $weeklyHours saat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = weeklyHoursSlider,
                    onValueChange = { weeklyHoursSlider = it },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Bu dersin haftalık ders saati (örn. Mat1 → 4 saat).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "$limit saat devamsızlık limiti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = limitSlider,
                    onValueChange = { limitSlider = it },
                    valueRange = 1f..(weeks * weeklyHours).coerceAtLeast(1).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Hocanızın izin verdiği toplam devamsızlık saatini girin (izinli sayılan saatler bu limite dahil edilmez).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            PremiumButton(onClick = { onConfirm(weeks, weeklyHours, limit) }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            PremiumTextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
