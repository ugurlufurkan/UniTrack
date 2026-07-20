package com.unitrack.app.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.random.Random

/**
 * Apple / Spotify / Notion / Arc Browser'ın zeminlerinde ortak olan çok hafif
 * "film grain" dokusu: düz bir rengin üstüne serpiştirilmiş, gerçek bir yüzey
 * hissi veren ~%2 opaklıkta gri-tonlu gürültü.
 *
 * Performans için: her karede rastgele piksel üretmek yerine, KÜÇÜK (64x64)
 * bir gürültü bitmap'i BİR KEZ üretilip [Shader.TileMode.REPEATED] ile
 * ekrana döşenir (tile'lanır). `drawWithCache`, boyut değişmediği sürece bu
 * bitmap'i yeniden oluşturmaz — yani gürültü, tıpkı gerçek bir doku gibi
 * ucuza her `draw` çağrısında tekrar kullanılır.
 */
fun Modifier.noiseOverlay(alpha: Float = 0.025f, tileSizePx: Int = 64): Modifier = this.drawWithCache {
    val bitmap = Bitmap.createBitmap(tileSizePx, tileSizePx, Bitmap.Config.ARGB_8888)
    val random = Random(seed = 42) // sabit seed: her recomposition'da aynı doku, "titreme" olmaz
    for (x in 0 until tileSizePx) {
        for (y in 0 until tileSizePx) {
            // Griler arası rastgele bir değer: yalnızca parlaklık (luma) değişiyor,
            // renk sapması yok — bu yüzden zemin rengini hiç bozmuyor.
            val v = random.nextInt(80, 255)
            bitmap.setPixel(x, y, android.graphics.Color.argb(255, v, v, v))
        }
    }

    val shader = ImageShader(
        image = bitmap.asImageBitmap(),
        tileModeX = androidx.compose.ui.graphics.TileMode.Repeated,
        tileModeY = androidx.compose.ui.graphics.TileMode.Repeated
    )

    val paint = Paint().apply {
        this.shader = shader
        this.alpha = alpha
        // Sadece parlaklık varyasyonu bindirmek için "overlay" yerine
        // "screen"e yakın bir his veren soft-light benzeri karışım —
        // BlendMode.Overlay altta koyu zeminlerde en doğal sonucu veriyor.
        this.blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
    }

    onDrawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            canvas.drawRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                paint = paint
            )
        }
    }
}
