package com.unitrack.app.ui.components

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [AmbientBackground] tarafından her karede kaydedilen (record edilen) zemin
 * görüntüsü — köşe ışıkları, floating orb'lar ve noise dahil, EKRANIN TAMAMI
 * kadar boyutta, kök (root) koordinatlarında bir "anlık görüntü" katmanı.
 *
 * [GlassCard], kendi ekran konumuna denk gelen kısmını bu katmandan kesip
 * (translate ederek) API 31+'da gerçekten bulanıklaştırıp arkasına çizer —
 * yani kartın "arkasında" gerçekten ambient ışıklar görünür ve bulanıktır
 * (gerçek "backdrop blur" / madde #6). Bu, üçüncü parti bir kütüphane (Haze
 * vb.) eklemeden, doğrudan Compose UI 1.7+'ın `GraphicsLayer` kayıt API'siyle
 * yapılıyor.
 *
 * BİLİNÇLİ SINIRLAMA: Katman yalnızca AmbientBackground'un kendi çizdiği
 * (statik/animasyonlu) zemin öğelerini içerir — kartın ARKASINDA duran başka
 * bir kart ya da kaydırılan liste içeriğini içermez (bu, tüm ekran
 * içeriğinin her karede yeniden kaydedilmesini gerektirir ve önemli bir
 * performans maliyeti getirir). Uygulamada GlassCard'lar zaten başka içeriğin
 * üstüne bindirilmiyor, bu yüzden bu kapsam kullanıcı deneyimi açısından
 * yeterli — gördüğü şey, arkasındaki ambient ışığın gerçekten bulanıklaşmış
 * hali.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

/**
 * Gerçek backdrop blur, `RenderEffect` API 31 (Android 12) ve üzerinde
 * çalışır. Altındaki sürümlerde GlassCard sessizce eski yarı saydam gradyan
 * yöntemine döner (bkz. GlassCard.kt).
 */
val isRealBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * [GlassCard] içindeki backdrop-blur mantığının paylaşılan hali — nav bar gibi
 * başka "cam" yüzeyler de aynı tekniği kullanabilsin diye buraya çıkarıldı.
 * Yalnızca [AmbientBackground]'un kaydettiği zemini bulanıklaştırıp bu
 * composable'ın kendi ekran konumuna göre arkasına çizer; kendi rengini/tonunu
 * eklemez — çağıran, blur'dan SONRA kendi scrim/tint'ini eklemeli (bkz.
 * GlassCard.kt ve UniTrackNavHost.kt'deki kullanım).
 */
fun Modifier.glassBlurBackdrop(blurRadius: Dp = 28.dp): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    this
        .onGloballyPositioned { coordinates ->
            positionInRoot = coordinates.positionInRoot()
        }
        .then(
            if (isRealBlurSupported && backdrop != null) {
                Modifier.drawWithContent {
                    val blurPx = with(density) { blurRadius.toPx() }
                    backdrop.renderEffect = BlurEffect(blurPx, blurPx)
                    backdrop.translationX = -positionInRoot.x
                    backdrop.translationY = -positionInRoot.y
                    drawLayer(backdrop)
                    // Katman paylaşımlı — sonraki kartın/yüzeyin yanlışlıkla
                    // bulanık zemin çizmemesi için hemen sıfırlıyoruz.
                    backdrop.renderEffect = null
                    backdrop.translationX = 0f
                    backdrop.translationY = 0f
                    drawContent()
                }
            } else {
                Modifier
            }
        )
}
