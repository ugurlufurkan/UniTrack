package com.unitrack.app.ui.gpa.simulator

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.GradeBandDto
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.GlassFab
import com.unitrack.app.ui.components.GradeChip
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.MetricCard
import com.unitrack.app.ui.components.PremiumButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.theme.Spacing

/**
 * "Ya X dersinden Y alırsam GANO'm ne olur?" ekranı.
 *
 * Tamamen client-side: backend'e hiçbir yeni istek/uç eklenmedi. Mevcut
 * GET /api/gpa sonucundaki TAMAMLANMIŞ derslerin kredi/puanları temel alınıp,
 * kullanıcının eklediği hayali derslerle (hiç kaydedilmeden, sadece bu
 * ekranda yaşayan state) projekte edilmiş bir GANO hesaplanıyor. Ayrıca bir
 * "hedef GANO'ya ulaşmak için ne almalıyım" hesaplayıcısı var.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaSimulatorScreen(
    onBack: () -> Unit,
    viewModel: GpaSimulatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val simulatedCourses by viewModel.simulatedCourses.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GANO Simülatörü") },
                navigationIcon = {
                    PressableIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (state is GpaSimulatorUiState.Success) {
                GlassFab(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Hayali ders ekle")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is GpaSimulatorUiState.Loading -> ListSkeleton(itemCount = 4)
                is GpaSimulatorUiState.Error -> {
                    ErrorState(
                        message = s.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GpaSimulatorUiState.Success -> {
                    val projected = viewModel.projectedGpa(s)
                    val delta = projected - s.currentGpa

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MetricCard(
                                    title = "Mevcut GANO",
                                    value = s.currentGpa.toFloat(),
                                    valueStyle = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Simüle Edilen GANO",
                                    value = projected.toFloat(),
                                    valueStyle = MaterialTheme.typography.headlineMedium,
                                    valueColor = when {
                                        simulatedCourses.isEmpty() -> Color.Unspecified
                                        delta > 0.001 -> if (delta > 0) MaterialTheme.colorScheme.primary else Color.Unspecified
                                        delta < -0.001 -> MaterialTheme.colorScheme.error
                                        else -> Color.Unspecified
                                    },
                                    caption = when {
                                        simulatedCourses.isEmpty() -> "Henüz hayali ders eklemedin"
                                        delta > 0.001 -> "+%.2f".format(delta)
                                        delta < -0.001 -> "%.2f".format(delta)
                                        else -> "Değişim yok"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            TargetGpaCard(
                                onOpen = { showTargetDialog = true }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Hayali Dersler",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (simulatedCourses.isNotEmpty()) {
                                    PremiumTextButton(onClick = { viewModel.clearAll() }) {
                                        Text("Tümünü Temizle")
                                    }
                                }
                            }
                        }

                        if (simulatedCourses.isEmpty()) {
                            item {
                                EmptyState(
                                    message = "\"+\" ile hayali bir ders ekle, GANO'nun\nnasıl değişeceğini anında gör.",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            itemsIndexed(simulatedCourses, key = { _, item -> item.id }) { index, course ->
                                StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                    SimulatedCourseRow(
                                        course = course,
                                        onDelete = { viewModel.removeCourse(course.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val successState = state as? GpaSimulatorUiState.Success

    if (showAddDialog && successState != null) {
        AddSimulatedCourseDialog(
            gradeScale = successState.gradeScale,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, credit, band ->
                viewModel.addCourse(name, credit, band.letter, band.point)
                showAddDialog = false
            }
        )
    }

    if (showTargetDialog && successState != null) {
        TargetGpaDialog(
            state = successState,
            onDismiss = { showTargetDialog = false },
            calculate = { targetGpa, plannedCredit ->
                viewModel.requiredPointForTarget(successState, targetGpa, plannedCredit)
            }
        )
    }
}

@Composable
private fun SimulatedCourseRow(course: SimulatedCourse, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(course.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${course.credit} Kredi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradeChip(letter = course.letter, point = course.point)
                PressableIconButton(onClick = onDelete, tint = MaterialTheme.colorScheme.error) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                }
            }
        }
    }
}

@Composable
private fun TargetGpaCard(onOpen: () -> Unit) {
    GlassCard(onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hedef GANO Hesaplayıcı", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Bir GANO hedefine ulaşmak için ne almalısın?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PremiumTextButton(onClick = onOpen) { Text("Hesapla") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSimulatedCourseDialog(
    gradeScale: List<GradeBandDto>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, credit: Int, band: GradeBandDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("3") }
    var expanded by remember { mutableStateOf(false) }
    var selectedBand by remember { mutableStateOf(gradeScale.firstOrNull()) }

    val creditValue = credit.toIntOrNull()
    val canConfirm = creditValue != null && creditValue > 0 && selectedBand != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Hayali Ders Ekle", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ders adı (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = credit,
                    onValueChange = { credit = it.filter { c -> c.isDigit() } },
                    label = { Text("Kredi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    OutlinedTextField(
                        value = selectedBand?.let { "${it.letter} (${it.point})" } ?: "Seç",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Beklenen Not") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumTextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) { Text("Seç") }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        gradeScale.forEach { band ->
                            DropdownMenuItem(
                                text = { Text("${band.letter} — ${band.point} puan") },
                                onClick = {
                                    selectedBand = band
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumButton(
                        onClick = { selectedBand?.let { onConfirm(name, creditValue ?: 0, it) } },
                        enabled = canConfirm
                    ) { Text("Ekle") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetGpaDialog(
    state: GpaSimulatorUiState.Success,
    onDismiss: () -> Unit,
    calculate: (targetGpa: Double, plannedCredit: Int) -> Double?
) {
    var targetGpaText by remember { mutableStateOf("") }
    var plannedCreditText by remember { mutableStateOf("") }

    val targetGpa = targetGpaText.replace(",", ".").toDoubleOrNull()
    val plannedCredit = plannedCreditText.toIntOrNull()

    val requiredPoint = if (targetGpa != null && plannedCredit != null && plannedCredit > 0) {
        calculate(targetGpa, plannedCredit)
    } else null

    val resultText: String? = requiredPoint?.let { req ->
        when {
            targetGpa != null && targetGpa > 4.0 ->
                "4'lük sistemde en yüksek GANO 4.0'dır."
            req <= 0.0 ->
                "Bu hedefi zaten geçmiş durumdasın — planlanan derslerde FF alsan bile hedefin altına düşmezsin."
            req > 4.0 ->
                "Bu hedefe belirtilen kredi ile ulaşmak matematiksel olarak mümkün değil (4.0'ın üzerinde bir ortalama gerekiyor)."
            else -> {
                val nearestBand = state.gradeScale
                    .filter { it.point >= req }
                    .minByOrNull { it.point }
                    ?: state.gradeScale.maxByOrNull { it.point }
                "Planladığın derslerden ortalama en az %.2f puan (yaklaşık %s ve üzeri) almalısın.".format(
                    req,
                    nearestBand?.letter ?: "—"
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Hedef GANO Hesaplayıcı", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Şu an %.2f GANO, %d kredin var.".format(state.currentGpa, state.currentCredit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = targetGpaText,
                    onValueChange = { targetGpaText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Hedef GANO (örn. 3.00)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = plannedCreditText,
                    onValueChange = { plannedCreditText = it.filter { c -> c.isDigit() } },
                    label = { Text("Kalan/planlanan toplam kredi") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (resultText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassCard {
                        Text(
                            resultText,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PremiumTextButton(onClick = onDismiss) { Text("Kapat") }
                }
            }
        }
    }
}