package com.unitrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.unitrack.app.R
import kotlinx.coroutines.delay

/**
 * "İşlem başarılı" anlarında (ders/dönem oluşturuldu, kaydedildi vb.) kısaca
 * beliren yeşil onay animasyonu. `res/raw/success_pulse.json` elle yazılmış,
 * küçük ve tamamen çevrimdışı çalışan basit bir Lottie dosyası — internet
 * gerektirmez, uygulamanın offline-first felsefesiyle tutarlı.
 *
 * NOT: Bu, LottieFiles'tan tasarımcı elinden çıkma bir animasyon DEĞİL,
 * elle yazılmış minimal bir yer tutucu (pulse + checkmark çizimi). Gerçekten
 * cilalı bir görünüm isteniyorsa lottiefiles.com'dan indirilecek bir JSON
 * bu dosyanın yerine konabilir; geri kalan her şey (bu composable, çağıran
 * ekranlar) hiç değişmeden çalışmaya devam eder.
 */
@Composable
fun SuccessPulse(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    size: androidx.compose.ui.unit.Dp = 96.dp
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success_pulse))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(size)
        )
    }

    LaunchedEffect(visible, progress) {
        if (visible && progress >= 1f) {
            onFinished()
        }
    }
}

/**
 * Bir işlem (ör. "Ders eklendi") tetiklendiğinde çağrılıp kısa süreliğine
 * `SuccessPulse`'ı gösteren ve kendi kendine kapanan yardımcı state.
 *
 * Kullanım:
 *   val successPulse = rememberSuccessPulseState()
 *   // kaydetme başarılı olduğunda:
 *   successPulse.trigger()
 *   // ekranda bir yerde:
 *   SuccessPulse(visible = successPulse.visible)
 */
@Composable
fun rememberSuccessPulseState(autoHideMillis: Long = 1200L): SuccessPulseState {
    // DİKKAT: bu değişkenin adı SuccessPulseState.visible ile AYNI OLMAMALI.
    // Aynı olsaydı, aşağıdaki "override val visible: Boolean get() = visible"
    // kendi kendini çağırır (üye, kapsayan yerel değişkeni gölgeler) ve
    // StackOverflowError'a yol açardı.
    val visibleState = remember { mutableStateOf(false) }

    LaunchedEffect(visibleState.value) {
        if (visibleState.value) {
            delay(autoHideMillis)
            visibleState.value = false
        }
    }

    return remember {
        object : SuccessPulseState {
            override val visible: Boolean get() = visibleState.value
            override fun trigger() {
                visibleState.value = true
            }
        }
    }
}

interface SuccessPulseState {
    val visible: Boolean
    fun trigger()
}
