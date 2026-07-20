package com.unitrack.app.ui.gpa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.GpaCourseResult
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.GradeChip
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.MetricCard
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaScreen(
    onNavigateToSimulator: () -> Unit = {},
    viewModel: GpaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
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
                is GpaUiState.Loading -> {
                    ListSkeleton(itemCount = 6)
                }
                is GpaUiState.Error -> {
                    ErrorState(
                        message = s.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GpaUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            haptic.refresh()
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                MetricCard(
                                    title = "Genel Not Ortalaması",
                                    value = s.data.gpa.toFloat()
                                )
                            }

                            item {
                                GlassCard(onClick = onNavigateToSimulator) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "GPA Simülatörü",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                "Hedef GANO hesapla, hayali derslerle senaryo dene",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (s.data.courses.isEmpty()) {
                                item {
                                    EmptyState(
                                        message = "GPA hesaplanacak ders bulunamadı.",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                itemsIndexed(s.data.courses) { index, course ->
                                    StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                        GpaCourseRow(course)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpaCourseRow(course: GpaCourseResult) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(course.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (course.average != null) {
                        "${course.credit} Kredi  •  Ort: ${String.format("%.1f", course.average)}"
                    } else {
                        "${course.credit} Kredi  •  Notlar henüz girilmedi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            GradeChip(letter = course.letter, point = course.point)
        }
    }
}