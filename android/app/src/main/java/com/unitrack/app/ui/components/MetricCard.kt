package com.unitrack.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.unitrack.app.ui.theme.Spacing

/**
 * Design System — MetricCard.
 *
 * "Büyük, tek bir sayıyı öne çıkaran" GlassCard kalıbı — GANO kartı
 * (`DashboardScreen.GpaHeroCard`) ve GPA sekmesindeki özet kartı gibi daha
 * önce iki ayrı yerde neredeyse birebir aynı şekilde elle yazılmış olan
 * "başlık + büyük animasyonlu sayı" düzenini tek bir yeniden kullanılabilir
 * bileşende topluyor. `StatCard` küçük/ikincil değerler için, `MetricCard`
 * ise ekranın odak noktası olan tek bir "hero" değer için.
 *
 * `caption` isteğe bağlı: alt bilgi (ör. "%.2f puan daha gerekiyor") veya
 * ekstra bir içerik (ör. ilerleme çubuğu) göstermek isteyen ekranlar
 * `extraContent` slotunu kullanabilir.
 */
@Composable
fun MetricCard(
    title: String,
    value: Float,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.displayMedium,
    valueColor: Color = Color.Unspecified,
    decimals: Int = 2,
    onClick: (() -> Unit)? = null,
    caption: String? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.xl)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedCounterText(
                targetValue = value,
                style = valueStyle,
                color = valueColor,
                decimals = decimals
            )

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                extraContent()
            }

            if (caption != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
