package com.unitrack.app.ui.transcriptimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.width
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.PressableIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptImportScreen(
    onBack: () -> Unit,
    viewModel: TranscriptImportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transkript ile Aktar") },
                navigationIcon = {
                    PressableIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val current = state) {
                is TranscriptImportUiState.Idle -> {
                    IdleContent(
                        onPickFile = { filePicker.launch("application/pdf") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TranscriptImportUiState.Parsing -> {
                    LoadingContent(
                        message = "Transkript okunuyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TranscriptImportUiState.Preview -> {
                    PreviewContent(
                        semesters = current.semesters,
                        onToggleCourse = viewModel::toggleCourse,
                        onToggleSemester = viewModel::toggleSemester,
                        onConfirm = viewModel::confirmImport,
                        onCancel = viewModel::reset
                    )
                }

                is TranscriptImportUiState.Importing -> {
                    LoadingContent(
                        message = "Dersler kaydediliyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TranscriptImportUiState.Success -> {
                    SuccessContent(
                        importedCourses = current.importedCourses,
                        onDone = onBack,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TranscriptImportUiState.Error -> {
                    ErrorState(
                        message = current.message,
                        onRetry = viewModel::reset,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.height(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Üniversitenden aldığın transkript PDF'ini yükle",
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tüm dönemlerin ve derslerin otomatik okunur, sen sadece onaylarsın. Tek tek elle girmene gerek kalmaz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPickFile) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PDF Seç")
        }
    }
}

@Composable
private fun LoadingContent(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SuccessContent(
    importedCourses: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "$importedCourses ders başarıyla aktarıldı",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) {
            Text("Tamam")
        }
    }
}

@Composable
private fun PreviewContent(
    semesters: List<PreviewSemesterItem>,
    onToggleCourse: (Int, Int) -> Unit,
    onToggleSemester: (Int, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val totalIncluded = semesters.sumOf { sem -> sem.courses.count { it.included } }
    val hasUnrecognized = semesters.any { sem -> sem.courses.any { !it.recognized && it.included } }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Okunan dersleri kontrol et, istemediklerini kaldır, sonra onayla.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )

        if (semesters.isEmpty()) {
            EmptyState(
                message = "Okunacak dönem bulunamadı.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(semesters) { semIndex, semester ->
                    SemesterPreviewCard(
                        semester = semester,
                        onToggleCourse = { courseIndex -> onToggleCourse(semIndex, courseIndex) },
                        onToggleAll = { included -> onToggleSemester(semIndex, included) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (hasUnrecognized) {
            Text(
                "İşaretli bazı dersler harf notu tanımadığımız (Muaf/Devamsız/Yeterli gibi) işaretler taşıyor — bunlar puansız, sadece isim ve kredi olarak aktarılacak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Vazgeç")
            }
            Button(
                onClick = onConfirm,
                enabled = totalIncluded > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("$totalIncluded Dersi Aktar")
            }
        }
    }
}

@Composable
private fun SemesterPreviewCard(
    semester: PreviewSemesterItem,
    onToggleCourse: (Int) -> Unit,
    onToggleAll: (Boolean) -> Unit
) {
    val allIncluded = semester.courses.all { it.included }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${semester.term} ${semester.year}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${semester.academicYearLabel} akademik yılı · ${semester.courses.size} ders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onToggleAll(!allIncluded) }) {
                    Text(if (allIncluded) "Hiçbirini seçme" else "Hepsini seç")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            semester.courses.forEachIndexed { index, course ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = course.included,
                        onCheckedChange = { onToggleCourse(index) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.name, style = MaterialTheme.typography.bodyMedium)
                        if (!course.recognized) {
                            Text(
                                "Puansız aktarılacak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Text(
                        "${course.credit} kredi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        course.grade,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (course.grade == "FF")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
