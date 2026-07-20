package com.unitrack.app.ui.semester

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.GlassFab
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.SearchField
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.components.refresh

private val TERMS = listOf("Güz", "Bahar", "Yaz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterScreen(
    viewModel: SemesterViewModel = hiltViewModel()
) {
    val semesters by viewModel.semesters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredSemesters = remember(semesters, searchQuery) {
        if (searchQuery.isBlank()) {
            semesters
        } else {
            semesters.filter { "${it.term} ${it.year}".contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dönem ekle")
            }
        },
        snackbarHost = {
            errorMessage?.let {
                Snackbar(
                    action = {
                        PremiumTextButton(onClick = { viewModel.clearError() }) { Text("Kapat") }
                    }
                ) { Text(it) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && semesters.isEmpty()) {
                ListSkeleton()
            } else {
                // Pull-to-refresh, isLoading'i doğrudan gösterge olarak kullanıyor:
                // refresh() zaten yeni veri gelene kadar mevcut semesters listesini
                // değiştirmiyor, o yüzden ayrı bir isRefreshing bayrağına gerek yok.
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = {
                        haptic.refresh()
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (semesters.isEmpty()) {
                        EmptyState(
                            message = "Henüz bir dönem eklemedin.\nSağ alttaki + butonuyla başla.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (semesters.size > 1) {
                                SearchField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    placeholder = "Dönem ara (ör. Güz 2025)...",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            if (filteredSemesters.isEmpty()) {
                                EmptyState(
                                    message = "\"$searchQuery\" ile eşleşen dönem bulunamadı.",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(filteredSemesters, key = { _, item -> item.id }) { index, semester ->
                                        StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                            SemesterRow(
                                                semester = semester,
                                                onDelete = {
                                                    haptic.click()
                                                    viewModel.deleteSemester(semester.id)
                                                }
                                            )
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

    if (showAddDialog) {
        AddSemesterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { year, term ->
                viewModel.addSemester(year, term)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SemesterRow(semester: SemesterDto, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${semester.term} ${semester.year}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PressableIconButton(onClick = onDelete, tint = MaterialTheme.colorScheme.error) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSemesterDialog(
    onDismiss: () -> Unit,
    onConfirm: (year: Int, term: String) -> Unit
) {
    var year by remember { mutableStateOf("") }
    var term by remember { mutableStateOf(TERMS.first()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Dönem") },
        text = {
            Column {

                OutlinedTextField(
                    value = year,
                    onValueChange = { input ->
                        if (input.length <= 4)
                            year = input.filter { it.isDigit() }
                    },
                    label = { Text("Yıl (ör. 2025)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box {

                    OutlinedTextField(
                        value = term,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dönem") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PremiumTextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Seç")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TERMS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    term = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PremiumTextButton(
                onClick = {
                    year.toIntOrNull()?.let {
                        onConfirm(it, term)
                    }
                },
                enabled = year.toIntOrNull() != null
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            PremiumTextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )}