package com.unitrack.app.ui.transcript

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.TranscriptEntryDto
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.PremiumButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GradeChip
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.SearchField
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.refresh
import com.unitrack.app.util.TranscriptPdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    welcomeName: String = "Öğrenci",
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                is TranscriptUiState.Loading -> {
                    ListSkeleton(itemCount = 6)
                }
                is TranscriptUiState.Error -> {
                    ErrorState(
                        message = s.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TranscriptUiState.Success -> {
                    val filteredEntries = remember(s.entries, searchQuery) {
                        if (searchQuery.isBlank()) {
                            s.entries
                        } else {
                            s.entries.filter { it.course.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (s.entries.isNotEmpty()) {
                            SearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = "Ders ara...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        if (s.entries.isEmpty()) {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    haptic.refresh()
                                    viewModel.refresh()
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                EmptyState(
                                    message = "Transkriptte gösterilecek ders yok.",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (filteredEntries.isEmpty()) {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    haptic.refresh()
                                    viewModel.refresh()
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                EmptyState(
                                    message = "\"$searchQuery\" ile eşleşen ders bulunamadı.",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    haptic.refresh()
                                    viewModel.refresh()
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(filteredEntries) { index, entry ->
                                        StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                            TranscriptRow(entry)
                                        }
                                    }
                                }
                            }

                            PremiumButton(
                                onClick = {
                                    isExporting = true
                                    scope.launch {
                                        try {
                                            val gpa = calculateGpa(s.entries)
                                            val uri = withContext(Dispatchers.IO) {
                                                TranscriptPdfExporter.export(
                                                    context = context,
                                                    studentName = welcomeName,
                                                    gpa = gpa,
                                                    entries = s.entries
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    TranscriptPdfExporter.buildShareIntent(uri),
                                                    "Transkripti Paylaş"
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "PDF oluşturulamadı: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                },
                                enabled = !isExporting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.height(0.dp))
                                Text(
                                    text = if (isExporting) "Oluşturuluyor..." else "  PDF Olarak Paylaş",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Notu tam girilmiş dersler üzerinden kredi ağırlıklı GANO hesaplar. */
private fun calculateGpa(entries: List<TranscriptEntryDto>): Double {
    val graded = entries.filter { it.average != null && it.point != null }
    val totalCredit = graded.sumOf { it.credit }
    if (totalCredit == 0) return 0.0
    val totalPoint = graded.sumOf { it.credit * (it.point ?: 0.0) }
    return Math.round((totalPoint / totalCredit) * 100) / 100.0
}

@Composable
private fun TranscriptRow(entry: TranscriptEntryDto) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // weight(1f): GradeChip için gereken alanı önce ona bırakıp geri
            // kalanı bu Column'a veriyor — aksi halde uzun ders adları rozetle
            // çarpışıp son harfler alt satıra sıkışarak kayıyordu.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.course,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (entry.average != null) {
                        "${entry.credit} Kredi  •  Ort: ${String.format("%.1f", entry.average)}"
                    } else {
                        "${entry.credit} Kredi  •  Notlar henüz girilmedi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            GradeChip(letter = entry.letter, point = entry.point)
        }
    }
}