package com.unitrack.app.ui.statistics

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.SemesterGpaDto
import com.unitrack.app.data.dto.StatisticsDto
import com.unitrack.app.ui.components.DonutSegment
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PremiumDonutChart
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.StatCard
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.refresh
import com.unitrack.app.ui.theme.StatusError
import com.unitrack.app.ui.theme.StatusSuccess
import com.unitrack.app.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İstatistikler") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            refreshError?.let {
                Snackbar(
                    action = {
                        PremiumTextButton(onClick = { viewModel.clearRefreshError() }) { Text("Kapat") }
                    }
                ) { Text(it) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is StatisticsUiState.Loading -> ListSkeleton(itemCount = 5)
                is StatisticsUiState.Error -> {
                    ErrorState(
                        message = s.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is StatisticsUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            haptic.refresh()
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        StatisticsContent(s.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(data: StatisticsDto) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Ders",
                    value = data.totalCourses,
                    icon = Icons.Filled.School,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Kredi",
                    value = data.totalCredits,
                    icon = Icons.Filled.Layers,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Geçilen",
                    value = data.passedCourses,
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            GlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ders Durumu",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val segments = buildList {
                        if (data.passedCourses > 0) {
                            add(DonutSegment(data.passedCourses.toFloat(), StatusSuccess, "Geçildi"))
                        }
                        if (data.failedCourses > 0) {
                            add(DonutSegment(data.failedCourses.toFloat(), StatusError, "Kaldı"))
                        }
                        if (data.ongoingCourses > 0) {
                            add(DonutSegment(data.ongoingCourses.toFloat(), StatusWarning, "Devam Ediyor"))
                        }
                    }

                    PremiumDonutChart(
                        segments = segments,
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.2f", data.overallAverage),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "genel ort.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendDot(color = StatusSuccess, label = "Geçildi (${data.passedCourses})")
                        LegendDot(color = StatusError, label = "Kaldı (${data.failedCourses})")
                        LegendDot(color = StatusWarning, label = "Devam Ediyor (${data.ongoingCourses})")
                    }
                }
            }
        }

        item {
            Text(
                "Dönem Bazlı GANO",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }

        if (data.semesterGpa.isEmpty()) {
            item {
                EmptyState(
                    message = "Henüz dönem bazlı GANO hesaplanacak notlandırılmış ders yok.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            itemsIndexed(data.semesterGpa) { index, entry ->
                StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                    SemesterGpaRow(entry)
                }
            }
        }
    }
}

@Composable
private fun SemesterGpaRow(entry: SemesterGpaDto) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(entry.semester, style = MaterialTheme.typography.titleMedium)
            Text(
                text = String.format("%.2f", entry.gpa),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
