package com.unitrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Bir liste öğesini, listedeki sırasına göre hafif bir gecikmeyle (index * [staggerMillis])
 * belirtip aşağıdan yukarı kayarak yerine oturtur. Dashboard'daki istatistik kartlarında
 * kullanılan efektin, tüm ekranlarda (Ders/Dönem/Transkript/GPA listeleri) tekrar
 * kullanılabilmesi için ortak bileşene çıkarılmış hali.
 *
 * NOT: Her LazyColumn/LazyRow item'ı composition'a girdiğinde tekrar tetiklenir (LaunchedEffect
 * key'i `Unit`), yani scroll ile ekrana yeniden giren bir öğe animasyonu tekrar oynatabilir.
 * Bu bilinçli bir tercih: "yeniden görünme" hissi de canlı bir liste için rahatsız edici değil,
 * aksine tutarlı bir mikro-etkileşim izlenimi veriyor.
 */
@Composable
fun StaggeredVisible(
    index: Int,
    modifier: Modifier = Modifier,
    staggerMillis: Long = 30L,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // Gecikmeyi ilk ~8 öğeyle sınırlıyoruz (maxStaggerIndex): aksi halde
    // 15-20 satırlık bir listede (Dönemler/Transkript gibi) son öğe ekrana
    // saniyelerce sonra geliyor ve sekme "yavaş açılıyormuş" gibi hissettiriyordu.
    // 8. öğeden sonrası hemen hemen aynı anda beliriyor.
    val maxStaggerIndex = 8
    LaunchedEffect(Unit) {
        delay(minOf(index, maxStaggerIndex) * staggerMillis)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        // Kayma (offset) artık spring ile — kart yerine hafifçe "esneyerek"
        // oturuyor (madde #12, Physics Animation), opaklık ise tween'de
        // kalıyor çünkü bir alfa değerinin zıplaması görsel olarak anlamsız.
        // StiffnessMedium: eski StiffnessLow, her satırın yerine oturması
        // gözle görülür şekilde uzun sürüyordu.
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { it / 2 }
    ) {
        content()
    }
}