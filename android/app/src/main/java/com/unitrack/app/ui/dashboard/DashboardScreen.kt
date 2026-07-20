package com.unitrack.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.unitrack.app.data.dto.AttendanceOverviewSummaryDto
import com.unitrack.app.data.dto.DashboardDto
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.data.local.ExamPeriod
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.ui.components.AnimatedCounterText
import com.unitrack.app.ui.components.AnimatedGpaProgress
import com.unitrack.app.ui.components.DonutSegment
import com.unitrack.app.ui.components.ErrorState
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.ListSkeleton
import com.unitrack.app.ui.components.PremiumDonutChart
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.StatCard
import com.unitrack.app.ui.components.StaggeredVisible
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.components.pressScale
import com.unitrack.app.ui.components.refresh
import com.unitrack.app.ui.theme.Spacing
import com.unitrack.app.ui.theme.rememberAdaptiveAccentColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    welcomeName: String,
    onNavigateToGpa: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToSemesters: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val targetGpa by viewModel.targetGpa.collectAsState()
    val examPeriod by viewModel.examPeriod.collectAsState()
    val calendarSummary by viewModel.calendarSummary.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showTargetDialog by remember { mutableStateOf(false) }
    var showExamPeriodDialog by remember { mutableStateOf(false) }

    // Dashboard, alt navigasyon çubuğunda sekmeler arasında geçişte
    // (saveState/restoreState) yeniden oluşturulmuyor; bu yüzden ViewModel
    // sadece ilk açılışta bir kere veri çekiyordu. Başka bir ekrandan
    // (örn. ders ekleme) geri dönüldüğünde ekran her RESUME olduğunda
    // veriyi tazeleyerek "0 ders" gibi bayat veri görünmesini engelliyoruz.
    // refresh() kullanıyoruz (load() değil) ki elimizde zaten veri varsa
    // ekran boşalıp yeniden yüklenmesin, sadece sessizce tazelensin.
    val currentViewModel = rememberUpdatedState(viewModel)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.value.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showTargetDialog) {
        EditTargetGpaDialog(
            currentTarget = targetGpa,
            onDismiss = { showTargetDialog = false },
            onConfirm = {
                viewModel.setTargetGpa(it)
                showTargetDialog = false
            }
        )
    }

    if (showExamPeriodDialog) {
        EditExamPeriodDialog(
            currentPeriod = examPeriod,
            onDismiss = { showExamPeriodDialog = false },
            onConfirm = { start, end -> viewModel.setExamPeriod(start, end) },
            onClear = { viewModel.clearExamPeriod() }
        )
    }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                text = "Merhaba, $welcomeName 👋",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "İşte genel akademik durumun",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            ThemeModeSelector(
                selected = themeMode,
                onSelect = {
                    haptic.click()
                    onThemeModeChange(it)
                }
            )

            com.unitrack.app.ui.settings.DataExportButton()

            Spacer(modifier = Modifier.height(6.dp))

            ExamPeriodStatusRow(
                examPeriod = examPeriod,
                onClick = {
                    haptic.click()
                    showExamPeriodDialog = true
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (val ui = state) {

                DashboardUiState.Loading -> {
                    ListSkeleton(itemCount = 4, contentPadding = PaddingValues(0.dp))
                }

                is DashboardUiState.Error -> {
                    ErrorState(
                        message = ui.message,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DashboardUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            haptic.refresh()
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DashboardContent(
                            data = ui.data,
                            targetGpa = targetGpa,
                            calendarSummary = calendarSummary,
                            onEditTarget = { showTargetDialog = true },
                            onNavigateToGpa = {
                                haptic.click()
                                onNavigateToGpa()
                            },
                            onNavigateToStatistics = {
                                haptic.click()
                                onNavigateToStatistics()
                            },
                            onNavigateToSemesters = {
                                haptic.click()
                                onNavigateToSemesters()
                            },
                            onNavigateToCourses = {
                                haptic.click()
                                onNavigateToCourses()
                            },
                            onNavigateToCalendar = {
                                haptic.click()
                                onNavigateToCalendar()
                            }
                        )
                    }
                }

            }

        }

    }

}

@Composable
private fun EditTargetGpaDialog(
    currentTarget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var sliderValue by remember(currentTarget) { mutableStateOf(currentTarget.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hedef GANO") },
        text = {
            Column {
                Text(
                    text = "Bu dönem için kendine bir hedef koy. Dashboard'daki ilerleme çubuğu buna göre hesaplanır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "%.2f".format(sliderValue),
                    style = MaterialTheme.typography.headlineMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..4f,
                    steps = 39 // 0.10 adımlarla
                )
            }
        },
        confirmButton = {
            PremiumTextButton(onClick = { onConfirm(sliderValue.toDouble()) }) {
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

/**
 * Panel'in en üstünde, tek satırlık basit bir Açık/Koyu/Sistem seçici.
 *
 * Daha önce uygulama sadece isSystemInDarkTheme()'e bağlıydı; kullanıcının
 * kendi tercihini kaydeden bir kontrol hiç yoktu. Segmented button yerine
 * (Material3'te deneysel/daha ağır) üç basit FilterChip kullanıldı — aynı
 * cam/blur dilinden bağımsız, sade ve her yerde çalışan bir kontrol.
 */
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeModeChip(
            label = "Sistem",
            icon = Icons.Default.BrightnessAuto,
            isSelected = selected == ThemeMode.SYSTEM,
            onClick = { onSelect(ThemeMode.SYSTEM) }
        )
        ThemeModeChip(
            label = "Açık",
            icon = Icons.Default.LightMode,
            isSelected = selected == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) }
        )
        ThemeModeChip(
            label = "Koyu",
            icon = Icons.Default.DarkMode,
            isSelected = selected == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) }
        )
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/**
 * Dashboard başlığının altında görünen küçük, tıklanabilir durum satırı.
 * Madde #25 (Weather Effect) için giriş noktası — AmbientBackground'daki
 * amber tonu bu ekranda ayarlanan aralığa göre devreye giriyor.
 */
@Composable
private fun ExamPeriodStatusRow(examPeriod: ExamPeriod?, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale("tr")) }
    val label = if (examPeriod != null) {
        "📚 Sınav haftası: ${examPeriod.start.format(formatter)} – ${examPeriod.end.format(formatter)}"
    } else {
        "Sınav haftanı ayarla"
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExamPeriodDialog(
    currentPeriod: ExamPeriod?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onClear: () -> Unit
) {
    var step by remember {
        mutableStateOf(if (currentPeriod == null) ExamPeriodStep.PickStart else ExamPeriodStep.Summary)
    }
    var pickedStart by remember { mutableStateOf(currentPeriod?.start) }
    var pickedEnd by remember { mutableStateOf(currentPeriod?.end) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr")) }

    when (step) {
        ExamPeriodStep.Summary -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Sınav Haftası") },
                text = {
                    Column {
                        Text(
                            text = "${pickedStart?.format(formatter)} – ${pickedEnd?.format(formatter)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bu aralıkta uygulamanın arka planı hafifçe amber tona kayar — " +
                                "sadece görsel bir hatırlatma, bildirim göndermez.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    PremiumTextButton(onClick = { step = ExamPeriodStep.PickStart }) {
                        Text("Değiştir")
                    }
                },
                dismissButton = {
                    Row {
                        PremiumTextButton(onClick = {
                            onClear()
                            onDismiss()
                        }) { Text("Kaldır") }
                        PremiumTextButton(onClick = onDismiss) { Text("Kapat") }
                    }
                }
            )
        }

        ExamPeriodStep.PickStart -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = pickedStart?.toUtcEpochMillis()
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                text = { DatePicker(state = state, title = { Text("Başlangıç tarihi") }) },
                confirmButton = {
                    PremiumTextButton(onClick = {
                        state.selectedDateMillis?.let { pickedStart = it.toUtcLocalDate() }
                        step = ExamPeriodStep.PickEnd
                    }) { Text("İleri") }
                },
                dismissButton = {
                    PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                }
            )
        }

        ExamPeriodStep.PickEnd -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = (pickedEnd ?: pickedStart)?.toUtcEpochMillis()
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                text = { DatePicker(state = state, title = { Text("Bitiş tarihi") }) },
                confirmButton = {
                    PremiumTextButton(onClick = {
                        val start = pickedStart
                        val end = state.selectedDateMillis?.toUtcLocalDate() ?: pickedEnd
                        if (start != null && end != null) {
                            onConfirm(start, end)
                        }
                        onDismiss()
                    }) { Text("Kaydet") }
                },
                dismissButton = {
                    PremiumTextButton(onClick = onDismiss) { Text("Vazgeç") }
                }
            )
        }
    }
}

private enum class ExamPeriodStep { Summary, PickStart, PickEnd }

/** DatePickerState UTC gece yarısı milisaniye bekler; LocalDate <-> Long dönüşümleri. */
private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun DashboardContent(
    data: DashboardDto,
    targetGpa: Double,
    calendarSummary: com.unitrack.app.data.dto.CalendarSummaryDto?,
    onEditTarget: () -> Unit,
    onNavigateToGpa: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSemesters: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            GpaHeroCard(
                gpa = data.gpa,
                targetGpa = targetGpa,
                onEditTarget = onEditTarget,
                onNavigateToGpa = onNavigateToGpa
            )
        }

        item {

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {

                val stats = listOf(
                    StatItem("Dönem", data.totalSemesters, Icons.Default.CalendarMonth, onClick = onNavigateToSemesters),
                    StatItem("Ders", data.totalCourses, Icons.Default.MenuBook, onClick = onNavigateToCourses),
                    StatItem("Kredi", data.totalCredits, Icons.Default.Star),
                    StatItem("Geçilen", data.passedCourses, Icons.Default.CheckCircle),
                    StatItem("Kalınan", data.failedCourses, Icons.Default.Cancel),
                    StatItem("Devam Eden", data.ongoingCourses, Icons.Default.HourglassTop),
                    StatItem("Haftalık Ders", data.weeklyLessonCount, Icons.Default.CalendarMonth, onClick = onNavigateToCalendar)
                )

                items(stats.size) { index ->
                    StaggeredStatCard(item = stats[index], index = index)
                }

            }

        }

        if (calendarSummary != null) {
            item {
                com.unitrack.app.ui.calendar.DashboardCalendarSummarySection(
                    summary = calendarSummary,
                    onClick = onNavigateToCalendar
                )
            }
        }

        if (data.totalCourses > 0) {
            item {
                CourseStatusDonutCard(
                    passed = data.passedCourses,
                    failed = data.failedCourses,
                    ongoing = data.ongoingCourses,
                    onClick = onNavigateToCourses
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToStatistics) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text("İstatistikler", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Dönem bazlı GANO ve ders dağılımı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val attendanceOverview = data.attendanceOverview
        if (attendanceOverview != null && attendanceOverview.totalCourses > 0) {
            item {
                AttendanceOverviewCard(overview = attendanceOverview)
            }
        }

    }

}

/**
 * Devamsızlık özeti — attendance/overview'dan gelen risk sayısını dashboard'a
 * taşıyan küçük uyarı kartı. Detay ekranı (haftalık işaretleme, ders bazlı
 * kırılım) ayrı bir "Devamsızlık" sekmesinde; burada sadece dikkat çekici bir
 * özet var, ayrıntıya inmiyoruz.
 */
@Composable
private fun AttendanceOverviewCard(
    overview: AttendanceOverviewSummaryDto
) {
    val warning = Color(0xFFFFC168)
    val isAtRisk = overview.atRiskCourses > 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = if (isAtRisk) warning else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = "Devamsızlık Durumu",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isAtRisk) {
                        "%d ders devamsızlık sınırına yaklaşıyor · Ortalama %%%.1f".format(
                            overview.atRiskCourses,
                            overview.averageAbsenceRate
                        )
                    } else {
                        "Tüm derslerde devamsızlık sınırın altındasın · Ortalama %%%.1f".format(
                            overview.averageAbsenceRate
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Madde #14 — Premium Charts: gerçek veriyle (DashboardDto'daki
 * passed/failed/ongoing — uydurma değil, backend'den gelen sayılar) çizilen
 * Apple Health tarzı glow'lu halka grafiği. Sağında renkli nokta + sayı
 * legend'ı var, tıklamaya gerek kalmadan grafiği okunaklı kılıyor.
 */
@Composable
private fun CourseStatusDonutCard(passed: Int, failed: Int, ongoing: Int, onClick: () -> Unit) {
    val success = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val warning = Color(0xFFFFC168)

    val segments = remember(passed, failed, ongoing) {
        buildList {
            if (passed > 0) add(DonutSegment(passed.toFloat(), success, "Geçilen"))
            if (failed > 0) add(DonutSegment(failed.toFloat(), error, "Kalınan"))
            if (ongoing > 0) add(DonutSegment(ongoing.toFloat(), warning, "Devam Eden"))
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumDonutChart(
                segments = segments,
                size = 96.dp,
                strokeWidth = 14.dp
            ) {
                Text(
                    text = (passed + failed + ongoing).toString(),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ders Durumu",
                    style = MaterialTheme.typography.titleMedium
                )
                DonutLegendRow(color = success, label = "Geçilen", count = passed)
                DonutLegendRow(color = error, label = "Kalınan", count = failed)
                DonutLegendRow(color = warning, label = "Devam Eden", count = ongoing)
            }
        }
    }
}

@Composable
private fun DonutLegendRow(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class StatItem(
    val label: String,
    val value: Int,
    val icon: ImageVector,
    // Madde #10 — Gesture Animation: bazı stat kartlarının (Dönem, Ders) doğrudan
    // karşılık geldiği bir sekme var — onlar tıklanabilir. "Kredi"/"Geçilen"/
    // "Kalınan"/"Devam Eden" gibi tek bir sekmeye karşılık gelmeyenler için
    // null bırakılır (uydurma bir navigasyon hedefi eklenmez).
    val onClick: (() -> Unit)? = null
)

/**
 * Her kart, listedeki sırasına göre hafif bir gecikmeyle (index * 60ms) belirir
 * ve aşağıdan yukarı kayarak yerine oturur — dashboard ilk açıldığında tüm
 * kartların birden "çat" diye belirmesi yerine sırayla akan bir his verir.
 */
@Composable
private fun StaggeredStatCard(item: StatItem, index: Int) {
    StaggeredVisible(index = index) {
        StatCard(
            label = item.label,
            value = item.value,
            icon = item.icon,
            onClick = item.onClick
        )
    }
}

@Composable
private fun GpaHeroCard(
    gpa: Double,
    targetGpa: Double,
    onEditTarget: () -> Unit,
    onNavigateToGpa: () -> Unit
) {

    // Kartın vurgu rengi, GANO'ya göre hafifçe yeşile/sarıya kayar (madde
    // #26 — Adaptive Accent). Tema bütünüyle değişmiyor, sadece bu iki
    // öğenin rengi.
    val accentColor by rememberAdaptiveAccentColor(gpa)

    // Madde #10 — Gesture Animation: kart artık tıklanabilir (GPA sekmesine
    // götürür) VE basılınca pressScale ile hafifçe küçülüp geri zıplıyor.
    // Önceden GlassCard'ın onClick/pressScale altyapısı hazırdı ama hiçbir
    // ekranda gerçekten kullanılmıyordu; burada ilk gerçek kullanımı.
    // Madde #7 — Hero Card: bu kart, altındaki küçük StatCard'lardan (düz
    // Card, titleLarge sayı) kasıtlı olarak çok daha büyük ve baskın —
    // GlassCard + displayLarge (57sp) rakam + daha geniş iç boşluk. Göz
    // önce buraya gidiyor, StatCard'lar destekleyici/ikincil kalıyor.
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToGpa) {

        Column(
            modifier = Modifier
                .padding(Spacing.xl)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Genel Not Ortalaman",
                    style = MaterialTheme.typography.bodyMedium
                )
                PressableIconButton(onClick = onEditTarget, size = 28.dp) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Hedefi düzenle",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedCounterText(
                targetValue = gpa.toFloat(),
                style = MaterialTheme.typography.displayLarge,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedGpaProgress(
                currentGpa = gpa,
                targetGpa = targetGpa,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val remaining = targetGpa - gpa
            Text(
                text = if (remaining > 0.005) {
                    "Hedefin %.2f — %.2f puan daha gerekiyor".format(targetGpa, remaining)
                } else {
                    "Hedefine ulaştın! 🎉 (%.2f)".format(targetGpa)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }

    }

}


