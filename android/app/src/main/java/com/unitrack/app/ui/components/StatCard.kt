package com.unitrack.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unitrack.app.ui.theme.Spacing

/**
 * Design System — StatCard.
 *
 * Küçük, ikon + sayaç + etiketten oluşan durum kartı (dashboard'daki "Dönem",
 * "Ders", "Kredi" vb. kutucuklar). Daha önce `DashboardScreen.kt` içinde
 * private bir composable'dı; buraya, resmi bileşen setinin bir parçası olarak
 * taşındı ki başka ekranlar da (ör. Transcript, Course özet satırları)
 * aynı kartı yeniden yazmadan kullanabilsin.
 *
 * BUG FIX: eski sürüm düz Material3 `Card` kullanıyordu — bu da uygulamanın
 * geri kalanındaki "cam" (glass) diliyle (bkz. GlassCard.kt) tutarsızdı;
 * dashboard'da StatCard'lar GpaHeroCard'ın hemen yanında farklı bir yüzey
 * gibi duruyordu. Artık diğer her yerde olduğu gibi `GlassCard` kullanıyor,
 * böylece blur/gölge/kenarlık dili bütün ekranda tek tip.
 */
@Composable
fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedCounterInt(targetValue = value)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
