package com.unitrack.app.ui.attendance

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.AttendanceCourseSummaryDto
import com.unitrack.app.data.dto.AttendanceOverviewDto
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.SearchField
import com.unitrack.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit = {},
    onOpenCourse: (courseId: String, courseName: String) -> Unit = { _, _ -> },
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Devamsızlık Takibi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    PressableIconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
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
            when (val state = uiState) {
                is AttendanceUiState.Loading -> ListSkeleton()
                is AttendanceUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
                is AttendanceUiState.Success -> {
                    if (state.overview.courses.size > 1) {
                        SearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Ders ara...",
                            modifier = Modifier.padding(top = Spacing.sm)
                        )
                    }
                    AttendanceContent(
                        overview = state.overview,
                        searchQuery = searchQuery,
                        onOpenCourse = onOpenCourse
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceContent(
    overview: AttendanceOverviewDto,
    searchQuery: String,
    onOpenCourse: (courseId: String, courseName: String) -> Unit
) {
    val filteredCourses = remember(overview.courses, searchQuery) {
        if (searchQuery.isBlank()) {
            overview.courses
        } else {
            overview.courses.filter { it.courseName.contains(searchQuery, ignoreCase = true) }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item {
            AttendanceOverviewCard(overview)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Dersler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        if (filteredCourses.isEmpty()) {
            item {
                EmptyState(
                    message = "\"$searchQuery\" ile eşleşen ders bulunamadı.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(filteredCourses) { course ->
                CourseAttendanceRow(
                    course = course,
                    onClick = { onOpenCourse(course.courseId, course.courseName) }
                )
            }
        }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

@Composable
private fun AttendanceOverviewCard(overview: AttendanceOverviewDto) {
    GlassCard {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                text = "Genel Durum",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ortalama devamsızlık",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%${"%.1f".format(overview.averageAbsenceRate)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (overview.averageAbsenceRate >= 15.0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Riskli ders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${overview.atRiskCourses} / ${overview.totalCourses} ders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overview.atRiskCourses > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseAttendanceRow(
    course: AttendanceCourseSummaryDto,
    onClick: () -> Unit
) {
    val warningColor = MaterialTheme.colorScheme.error
    val safeColor = MaterialTheme.colorScheme.tertiary
    val accentColor = if (course.isAtRisk) warningColor else safeColor
    val absenceProgress = (course.absenceRatePercent / 100.0).toFloat().coerceIn(0f, 1f)

    GlassCard(onClick = onClick) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "${course.absentHours}/${course.totalCourseHours} sa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            // Progress bar showing absence rate vs threshold
            LinearProgressIndicator(
                progress = { absenceProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AttendanceStat(label = "Katıldı", value = course.attendedHours, color = safeColor)
                    AttendanceStat(label = "Devamsız", value = course.absentHours, color = warningColor)
                    AttendanceStat(label = "İzinli", value = course.excusedHours, color = MaterialTheme.colorScheme.secondary)
                }
                if (course.isAtRisk) {
                    AtRiskChip()
                } else if (course.remainingAllowedHours != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = safeColor
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${course.remainingAllowedHours} sa hak kaldı",
                            style = MaterialTheme.typography.labelSmall,
                            color = safeColor
                        )
                    }
                } else {
                    Text(
                        text = "Limit ayarlanmadı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun AtRiskChip() {
    val errorColor = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(errorColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = errorColor
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "Devamsızlık riski",
                style = MaterialTheme.typography.labelSmall,
                color = errorColor
            )
        }
    }
}
