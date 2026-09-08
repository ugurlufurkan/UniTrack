package com.unitrack.app.ui.nav

import com.unitrack.app.ui.gpa.simulator.GpaSimulatorScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.components.glassBlurBackdrop
import com.unitrack.app.ui.components.isRealBlurSupported
import com.unitrack.app.ui.attendance.AttendanceScreen
import com.unitrack.app.ui.attendance.CourseAttendanceScreen
import com.unitrack.app.ui.calendar.CalendarScreen
import com.unitrack.app.ui.course.CourseScreen
import com.unitrack.app.ui.dashboard.DashboardScreen
import com.unitrack.app.ui.gpa.GpaScreen
import com.unitrack.app.ui.semester.SemesterScreen
import com.unitrack.app.ui.settings.SettingsScreen
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.ui.statistics.StatisticsScreen
import com.unitrack.app.ui.task.TaskScreen
import com.unitrack.app.ui.transcript.TranscriptScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

private sealed class Tab(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Dashboard : Tab("dashboard", "Panel", Icons.Filled.Home, Icons.Outlined.Home)
    object Semesters : Tab("semesters", "Dönemler", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Courses : Tab("courses", "Dersler", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
    object Calendar : Tab("calendar", "Takvim", Icons.Filled.Event, Icons.Outlined.Event)
    object Gpa : Tab("gpa", "GPA", Icons.Filled.Star, Icons.Outlined.Star)
    object Transcript : Tab("transcript", "Transkript", Icons.Filled.Info, Icons.Outlined.Info)
    object Tasks : Tab("tasks", "Görevler", Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object Attendance : Tab("attendance", "Devamsızlık", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable)
}

private val TABS = listOf(
    Tab.Dashboard,
    Tab.Semesters,
    Tab.Courses,
    Tab.Calendar,
    Tab.Gpa,
    Tab.Transcript,
    Tab.Tasks,
    Tab.Attendance
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniTrackNavHost(
    welcomeName: String,
    onLogout: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val navController: NavHostController = rememberNavController()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("UniTrack") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    PressableIconButton(onClick = {
                        haptic.click()
                        navController.navigate("settings")
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
                    }
                    PressableIconButton(onClick = {
                        haptic.click()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap")
                    }
                }
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            // Çubuğun arkaplanı için "surface" yerine bilinçli olarak daha
            // kalkık (elevated) bir yüzey rengi kullanılıyor: koyu temada
            // background (#08080D) ile surface (#121218) birbirine çok
            // yakın tonlar olduğundan, şeffaf "surface" ile çizilen çubuk
            // gövdeyle neredeyse aynı siyaha karışıp seçilemez hale
            // geliyordu. surfaceVariant hem koyu hem açık temada gövdeden
            // net ayrışacak şekilde tanımlı.
            val barColor = MaterialTheme.colorScheme.surfaceVariant
            val primaryColor = MaterialTheme.colorScheme.primary
            val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            val dividerColor = MaterialTheme.colorScheme.outline

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBlurBackdrop(blurRadius = 24.dp)
                    .background(
                        barColor.copy(alpha = if (isRealBlurSupported) 0.85f else 0.97f)
                    )
                    .drawBehind {
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                TABS.forEach { tab ->
                    val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true

                    // Seçiliyse ana renk (mavi), değilse temaya duyarlı
                    // onSurfaceVariant — koyu temada açık gri (net görünür),
                    // açık temada koyu gri (orada da net görünür).
                    val contentColor = if (selected) {
                        primaryColor
                    } else {
                        unselectedColor
                    }

                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Column(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .widthIn(min = 64.dp)
                                .drawBehind {
                                    if (selected) {
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.2f),
                                            radius = size.minDimension * 0.75f,
                                            center = center.copy(y = center.y - 4.dp.toPx())
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Sorunlu AnimatedNavIcon yerine doğrudan renk alan güvenilir Icon bileşeni
                            Icon(
                                imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Dashboard.route) {
                DashboardScreen(
                    welcomeName = welcomeName,
                    onNavigateToGpa = {
                        navController.navigate(Tab.Gpa.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToStatistics = {
                        navController.navigate("statistics")
                    },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToSemesters = {
                        navController.navigate(Tab.Semesters.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCourses = {
                        navController.navigate(Tab.Courses.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Tab.Calendar.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Tab.Semesters.route) { entry ->
                // BUG FIX: transkript aktarımı bittiğinde bu sekmelerin
                // ViewModel'leri (aynı back stack entry'sinde yaşadıkları
                // için) kendiliğinden yenilenmiyordu — yeni eklenen
                // dönemler/dersler/GPA görünmüyordu. transcript_import
                // ekranı "transcript_imported" bayrağını true yaparak geri
                // dönüyor, ilgili her sekme bunu görüp bir kere refresh
                // olup bayrağı sıfırlıyor.
                val shouldRefresh by entry.savedStateHandle
                    .getStateFlow("transcript_imported", false)
                    .collectAsState()
                SemesterScreen(
                    refreshSignal = shouldRefresh,
                    onRefreshHandled = { entry.savedStateHandle["transcript_imported"] = false }
                )
            }
            composable(Tab.Courses.route) { entry ->
                val shouldRefresh by entry.savedStateHandle
                    .getStateFlow("transcript_imported", false)
                    .collectAsState()
                CourseScreen(
                    onNavigateToTranscriptImport = { navController.navigate("transcript_import") },
                    refreshSignal = shouldRefresh,
                    onRefreshHandled = { entry.savedStateHandle["transcript_imported"] = false }
                )
            }
            composable("transcript_import") {
                com.unitrack.app.ui.transcriptimport.TranscriptImportScreen(
                    onBack = {
                        // Önceki (Courses) back stack entry'sine "veriler
                        // değişmiş olabilir" sinyali bırakıyoruz. Diğer
                        // sekmeler (Semesters/Gpa/Transcript) de kendi
                        // entry'lerinde aynı bayrağı okuyup tazeleniyor.
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("transcript_imported", true)
                        // Diğer sekmeler bu oturumda hiç açılmamış olabilir —
                        // o zaman back stack'te henüz yoklar ve
                        // getBackStackEntry çökme fırlatır. Sessizce atla,
                        // zaten ilk açıldıklarında init{} kendi refresh()'ini
                        // çalıştırıp güncel veriyi getirecek.
                        listOf(Tab.Semesters.route, Tab.Gpa.route, Tab.Transcript.route).forEach { route ->
                            runCatching {
                                navController.getBackStackEntry(route)
                                    .savedStateHandle["transcript_imported"] = true
                            }
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Tab.Calendar.route) { CalendarScreen() }
            composable(Tab.Gpa.route) { entry ->
                val shouldRefresh by entry.savedStateHandle
                    .getStateFlow("transcript_imported", false)
                    .collectAsState()
                GpaScreen(
                    onNavigateToSimulator = {
                        navController.navigate("gpa_simulator")
                    },
                    refreshSignal = shouldRefresh,
                    onRefreshHandled = { entry.savedStateHandle["transcript_imported"] = false }
                )
            }
            composable("gpa_simulator") {
                GpaSimulatorScreen(onBack = { navController.popBackStack() })
            }
            composable("statistics") {
                StatisticsScreen()
            }
            composable(Tab.Transcript.route) { entry ->
                val shouldRefresh by entry.savedStateHandle
                    .getStateFlow("transcript_imported", false)
                    .collectAsState()
                TranscriptScreen(
                    welcomeName = welcomeName,
                    refreshSignal = shouldRefresh,
                    onRefreshHandled = { entry.savedStateHandle["transcript_imported"] = false }
                )
            }

            composable(Tab.Tasks.route) {
                TaskScreen(onBack = { navController.popBackStack() })
            }

            composable(Tab.Attendance.route) {
                AttendanceScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCourse = { courseId, courseName ->
                        val encoded = URLEncoder.encode(courseName, "UTF-8")
                        navController.navigate("attendance/course/$courseId/$encoded")
                    }
                )
            }

            composable(
                route = "attendance/course/{courseId}/{courseName}",
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("courseName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                val courseName = URLDecoder.decode(
                    backStackEntry.arguments?.getString("courseName") ?: "",
                    "UTF-8"
                )
                CourseAttendanceScreen(
                    courseId = courseId,
                    courseName = courseName,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}