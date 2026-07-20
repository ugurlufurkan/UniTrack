package com.unitrack.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.unitrack.app.MainActivity
import com.unitrack.app.data.dto.TaskItemDto
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ana ekran widget'ı: GPA'yi ve önümüzdeki 7 gün içinde teslim tarihi olan,
 * henüz bitmemiş görev sayısını gösterir.
 *
 * Neden bu ikisi (GPA + yaklaşan görev sayısı): kullanıcıdan "sen karar ver"
 * yanıtı geldi. Widget'lar küçük ve tek bakışta anlaşılır olmalı; devamsızlık
 * uyarısı sadece riskli dönemlerde anlamlıyken, GPA + yaklaşan görev sayısı
 * HER GÜN işe yarayan, her zaman dolu iki rakam. Haftalık ders programı da
 * düşünüldü ama bir 2x2 widget'a (ders adı + saat + yer) sığmıyor.
 *
 * Widget her zaman koyu/cam temada gösterilir (sistem açık/koyu temasından
 * bağımsız) — uygulamanın kendi kimliğiyle tutarlı, sabit bir görünüm.
 */
class UniTrackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = context.widgetEntryPoint()
        val token = entryPoint.authPreferences().accessToken.first()

        val content: WidgetContent = if (token.isNullOrBlank()) {
            WidgetContent.LoggedOut
        } else {
            try {
                val gpa = entryPoint.academicRepository().getGpa().gpa
                val tasks = entryPoint.taskRepository().getAllTasks()
                val dueSoonCount = tasks.count { it.isDueWithinNextWeek() }
                WidgetContent.Data(gpa = gpa, dueSoonCount = dueSoonCount)
            } catch (_: Exception) {
                WidgetContent.Error
            }
        }

        provideContent {
            UniTrackWidgetContent(content)
        }
    }
}

private sealed class WidgetContent {
    object LoggedOut : WidgetContent()
    object Error : WidgetContent()
    data class Data(val gpa: Double, val dueSoonCount: Int) : WidgetContent()
}

private fun TaskItemDto.isDueWithinNextWeek(): Boolean {
    if (status == "completed" || status == "cancelled") return false
    val due = try {
        Instant.parse(startAt).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: Exception) {
        return false
    }
    val today = LocalDate.now()
    return !due.isBefore(today) && due.isBefore(today.plusDays(8))
}

private val WidgetBackground = Color(0xFF14141C)
private val WidgetTextDim = Color(0xFFA0A0AD)
private val WidgetAccent = Color(0xFF6C8CFF)

@Composable
private fun UniTrackWidgetContent(content: WidgetContent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            text = "UniTrack",
            style = TextStyle(color = ColorProvider(WidgetTextDim), fontSize = 11.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        when (content) {
            is WidgetContent.LoggedOut -> {
                Text(
                    text = "Giriş yapılmadı",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            is WidgetContent.Error -> {
                Text(
                    text = "Veriler yüklenemedi",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            is WidgetContent.Data -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = String.format("%.2f", content.gpa),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "GANO",
                            style = TextStyle(color = ColorProvider(WidgetTextDim), fontSize = 10.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(20.dp))

                    Column {
                        Text(
                            text = content.dueSoonCount.toString(),
                            style = TextStyle(
                                color = ColorProvider(WidgetAccent),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "7 gün içinde görev",
                            style = TextStyle(color = ColorProvider(WidgetTextDim), fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}

class UniTrackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UniTrackWidget()
}
