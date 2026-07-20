package com.unitrack.app.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.unitrack.app.data.dto.CourseComponentDto
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.GradeBandDto
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.ui.components.EmptyState
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.GlassFab
import com.unitrack.app.ui.components.GradeChip
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.SearchField
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.SuccessPulse
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.components.refresh
import com.unitrack.app.ui.components.rememberSuccessPulseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val defaultGradeScale by viewModel.defaultGradeScale.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<CourseDto?>(null) }
    var showDefaultScaleDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredCourses = remember(courses, searchQuery) {
        if (searchQuery.isBlank()) {
            courses
        } else {
            courses.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    val successPulse = rememberSuccessPulseState()

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = {
                if (semesters.isNotEmpty()) {
                    showAddDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Ders ekle")
            }
        },
        snackbarHost = {
            errorMessage?.let {
                Snackbar(action = {
                    PremiumTextButton(onClick = { viewModel.clearError() }) { Text("Kapat") }
                }) { Text(it) }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && courses.isEmpty() && semesters.isEmpty() -> {
                    ListSkeleton()
                }
                semesters.isEmpty() -> {
                    EmptyState(
                        message = "Ders ekleyebilmek için önce bir dönem oluşturmalısın.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            PremiumTextButton(onClick = { showDefaultScaleDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Varsayılan Not Skalası")
                            }
                        }

                        if (courses.isNotEmpty()) {
                            SearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = "Ders ara...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Pull-to-refresh, isLoading'i doğrudan gösterge olarak kullanıyor:
                        // refresh() zaten yeni veri gelene kadar mevcut courses listesini
                        // değiştirmiyor, o yüzden ayrı bir isRefreshing bayrağına gerek yok.
                        PullToRefreshBox(
                            isRefreshing = isLoading,
                            onRefresh = {
                                haptic.refresh()
                                viewModel.refresh()
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            if (courses.isEmpty()) {
                                EmptyState(
                                    message = "Henüz ders eklemedin.\nSağ alttaki + butonuyla başla.",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (filteredCourses.isEmpty()) {
                                EmptyState(
                                    message = "\"$searchQuery\" ile eşleşen ders bulunamadı.",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(filteredCourses, key = { _, item -> item.id }) { index, course ->
                                        StaggeredVisible(index = index, modifier = Modifier.animateItem()) {
                                            CourseRow(
                                                course = course,
                                                onEdit = { editingCourse = course },
                                                onDelete = {
                                                    haptic.click()
                                                    viewModel.deleteCourse(course.id)
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

            SuccessPulse(
                visible = successPulse.visible,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    if (showAddDialog) {
        CourseFormDialog(
            title = "Yeni Ders",
            semesters = semesters,
            initialCourse = null,
            defaultGradeScale = defaultGradeScale,
            onDismiss = { showAddDialog = false },
            onConfirm = { semesterId, name, credit, components, gradeScale ->
                viewModel.addCourse(semesterId, name, credit, components, gradeScale) { success ->
                    if (success) {
                        successPulse.trigger()
                        showAddDialog = false
                    }
                    // Başarısızsa dialog açık kalır, hata snackbar'da zaten gösteriliyor.
                }
            }
        )
    }

    editingCourse?.let { course ->
        CourseFormDialog(
            title = "Dersi Düzenle",
            semesters = semesters,
            initialCourse = course,
            defaultGradeScale = defaultGradeScale,
            onDismiss = { editingCourse = null },
            onConfirm = { _, name, credit, components, gradeScale ->
                viewModel.updateCourse(course.id, name, credit, components, gradeScale) { success ->
                    if (success) {
                        successPulse.trigger()
                        editingCourse = null
                    }
                }
            }
        )
    }

    if (showDefaultScaleDialog) {
        DefaultGradeScaleDialog(
            currentScale = defaultGradeScale,
            onDismiss = { showDefaultScaleDialog = false },
            onSave = { scale ->
                viewModel.setDefaultGradeScale(scale) { success ->
                    if (success) showDefaultScaleDialog = false
                }
            }
        )
    }
}

@Composable
private fun CourseRow(course: CourseDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = course.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    PressableIconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                    }
                    PressableIconButton(onClick = onDelete, tint = MaterialTheme.colorScheme.error) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {}, label = { Text("${course.credit} Kredi") })
                if (course.letterGrade != null || course.average != null) {
                    GradeChip(letter = course.letterGrade, point = course.gradePoint)
                }
                course.average?.let {
                    AssistChip(onClick = {}, label = { Text("Ort: ${String.format("%.1f", it)}") })
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = course.components.joinToString("  •  ") { c ->
                    "${c.name} (%${c.weight.toInt()}): ${c.score?.let { String.format("%.0f", it) } ?: "-"}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ComponentFormState(
    val id: String?,
    var name: String,
    var weight: String,
    var score: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseFormDialog(
    title: String,
    semesters: List<SemesterDto>,
    initialCourse: CourseDto?,
    defaultGradeScale: List<GradeBandDto>,
    onDismiss: () -> Unit,
    onConfirm: (
        semesterId: String,
        name: String,
        credit: Int,
        components: List<CourseComponentDto>,
        gradeScale: List<GradeBandDto>?
    ) -> Unit
) {
    val initialSemester = semesters.firstOrNull {
        it.id == initialCourse?.semesterId
    } ?: semesters.first()

    var selectedSemester by remember { mutableStateOf(initialSemester) }
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var credit by remember { mutableStateOf(initialCourse?.credit?.toString() ?: "") }

    val components = remember {
        mutableStateListOf<ComponentFormState>().apply {
            if (initialCourse != null && initialCourse.components.isNotEmpty()) {
                initialCourse.components.forEach {
                    add(ComponentFormState(it.id, it.name, it.weight.toInt().toString(), it.score?.toString() ?: ""))
                }
            } else {
                add(ComponentFormState(null, "Vize", "40", ""))
                add(ComponentFormState(null, "Final", "60", ""))
            }
        }
    }

    var useCustomScale by remember { mutableStateOf(initialCourse?.gradeScale != null) }
    var scaleErrorText by remember { mutableStateOf<String?>(null) }
    val scaleBands = remember {
        mutableStateListOf<GradeBandFormState>().apply {
            val source = initialCourse?.gradeScale ?: defaultGradeScale
            addAll(source.toFormState())
        }
    }

    val totalWeight = components.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }
    val weightValid = kotlin.math.abs(totalWeight - 100.0) < 0.01

    val formScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f) // Ekranın %90'ını kaplar
                .imePadding(), // Klavye açıldığında içeriğin üstüne binmesini engeller
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Başlık Alanı (Sabit)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
                )

                // Form Alanı (Kaydırılabilir)
                Column(
                    modifier = Modifier
                        .weight(1f) // Ekranın kalan kısmını doldurur ve kaydırmayı aktifleştirir
                        .verticalScroll(formScrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    if (initialCourse == null) {
                        Box {
                            OutlinedTextField(
                                value = "${selectedSemester.term} ${selectedSemester.year}",
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
                                semesters.forEach { semester ->
                                    DropdownMenuItem(
                                        text = { Text("${semester.term} ${semester.year}") },
                                        onClick = {
                                            selectedSemester = semester
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ders adı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = credit,
                        onValueChange = { credit = it.filter { c -> c.isDigit() } },
                        label = { Text("Kredi") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Bileşenler (Vize, Final, Proje, Quiz vb.)", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    components.forEachIndexed { index, comp ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = comp.name,
                                onValueChange = { components[index] = comp.copy(name = it) },
                                label = { Text("Ad") },
                                modifier = Modifier.weight(1.4f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = comp.weight,
                                onValueChange = { components[index] = comp.copy(weight = it.filter { c -> c.isDigit() } ) },
                                label = { Text("%") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = comp.score,
                                onValueChange = { components[index] = comp.copy(score = it.filter { c -> c.isDigit() }) },
                                label = { Text("Puan") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                            PressableIconButton(onClick = { components.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Bileşeni sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    PremiumTextButton(onClick = { components.add(ComponentFormState(null, "", "0", "")) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bileşen ekle")
                    }

                    Text(
                        text = "Toplam ağırlık: ${totalWeight.toInt()}%" + if (!weightValid) " — 100% olmalı!" else " ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (weightValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useCustomScale,
                            onCheckedChange = { checked ->
                                useCustomScale = checked
                                if (checked && scaleBands.isEmpty()) {
                                    scaleBands.addAll(defaultGradeScale.toFormState())
                                }
                            }
                        )
                        Text("Bu ders için özel harf notu skalası kullan")
                    }

                    if (useCustomScale) {
                        Spacer(modifier = Modifier.height(6.dp))
                        GradeScaleEditor(
                            bands = scaleBands,
                            onAdd = {
                                // Min. puanı hep "0" ile doldurmak, zaten var olan 0'lı satırla
                                // anında çakışıp "aynı minimum puana sahip iki aralık olamaz"
                                // hatasına yol açıyordu — kullanıcı fark etmeden. Boş bırakıp
                                // kullanıcıyı bilinçli bir değer girmeye zorluyoruz.
                                scaleBands.add(GradeBandFormState("", "", "0"))
                                coroutineScope.launch {
                                    formScrollState.animateScrollTo(formScrollState.maxValue)
                                }
                            },
                            onRemove = { index -> scaleBands.removeAt(index) },
                            onChange = { index, updated -> scaleBands[index] = updated }
                        )
                        scaleErrorText?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Alt Butonlar Alanı (Sabit)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumTextButton(
                        onClick = {
                            val creditInt = credit.toIntOrNull()
                            val componentDtos = components.mapNotNull { c ->
                                val weight = c.weight.toDoubleOrNull()
                                if (c.name.isBlank() || weight == null) null
                                else CourseComponentDto(
                                    id = c.id,
                                    name = c.name.trim(),
                                    weight = weight,
                                    score = c.score.toDoubleOrNull()
                                )
                            }
                            val gradeScale = if (useCustomScale) {
                                scaleErrorText = null
                                scaleBands.toDtoOrNull { msg -> scaleErrorText = msg }
                            } else null

                            if (name.isNotBlank() && creditInt != null && weightValid &&
                                componentDtos.size == components.size &&
                                (!useCustomScale || gradeScale != null)
                            ) {
                                onConfirm(selectedSemester.id, name.trim(), creditInt, componentDtos, gradeScale)
                            }
                        },
                        enabled = name.isNotBlank() && credit.toIntOrNull() != null && weightValid && components.isNotEmpty()
                    ) { Text(if (initialCourse == null) "Ekle" else "Kaydet") }
                }
            }
        }
    }
}

@Composable
private fun DefaultGradeScaleDialog(
    currentScale: List<GradeBandDto>,
    onDismiss: () -> Unit,
    onSave: (List<GradeBandDto>?) -> Unit
) {
    val bands = remember {
        mutableStateListOf<GradeBandFormState>().apply { addAll(currentScale.toFormState()) }
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .imePadding(), // Klavye açıldığında Kaydet butonunu ve son satırı gizlememesi için
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Varsayılan Not Skalası",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Buradaki aralıklar, özel skala tanımlamadığın tüm derslerde kullanılır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GradeScaleEditor(
                        bands = bands,
                        onAdd = {
                            bands.add(GradeBandFormState("", "", "0"))
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        onRemove = { index -> bands.removeAt(index) },
                        onChange = { index, updated -> bands[index] = updated }
                    )
                    errorText?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumTextButton(onClick = {
                        errorText = null
                        val dto = bands.toDtoOrNull { msg -> errorText = msg }
                        if (dto != null) onSave(dto)
                    }) { Text("Kaydet") }
                }
            }
        }
    }
}