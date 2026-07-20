package com.unitrack.app

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import com.unitrack.app.widget.UniTrackWidget
import kotlinx.coroutines.launch
import com.unitrack.app.ui.auth.AuthState
import com.unitrack.app.ui.auth.AuthViewModel
import com.unitrack.app.ui.auth.LoginScreen
import com.unitrack.app.ui.components.AmbientBackground
import com.unitrack.app.ui.nav.UniTrackNavHost
import com.unitrack.app.ui.theme.AmbientMoodViewModel
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.ui.theme.ThemeViewModel
import com.unitrack.app.ui.theme.UniTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Compose içindeki hiltViewModel() ile birebir aynı örnek (Activity'ye scope'lu).
    // Splash screen'in ne zaman kapanacağına karar verebilmek için setContent'ten önce lazım.
    private val authViewModel: AuthViewModel by viewModels()

    // AmbientBackground, DataStore/Hilt'ten habersiz kalsın diye (kendi başına
    // yeniden kullanılabilir bir bileşen olarak) "sınav haftası mı?" sorusunun
    // cevabı burada, kök seviyede üretilip parametre olarak aşağı veriliyor.
    private val ambientMoodViewModel: AmbientMoodViewModel by viewModels()

    // Kullanıcının Panel'den seçtiği Açık/Koyu/Sistem tema tercihi; tek
    // kaynağı burası, UniTrackTheme'e buradan aşağı akıyor.
    private val themeViewModel: ThemeViewModel by viewModels()

    // Takvim hatırlatma bildirimlerinin gerçekten gösterilebilmesi için
    // Android 13+'ta çalışma zamanında istenmesi zorunlu izin. Reddedilirse
    // uygulama normal çalışmaya devam eder — sadece hatırlatmalar sessiz kalır.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sonuç bilgisi şu an ayrı gösterilmiyor */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() super.onCreate()'den ÖNCE çağrılmalı.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // AuthViewModel oturum kontrolünü (restoreSession) bitirene kadar splash ekranda kalsın.
        // Böylece zaten giriş yapmış bir kullanıcı asla LoginScreen'i görmez.
        splashScreen.setKeepOnScreenCondition {
            authViewModel.authState.value is AuthState.Checking
        }

        // Madde #20 — Hero/Splash animasyonu: sistemin varsayılan (düz "kaybol")
        // splash kapanışı yerine, marka ikonu hafifçe büyüyüp yukarı kayarak
        // ve saydamlaşarak kapanıyor. `onSplashExitAnimationEnd()` çağrısı
        // ZORUNLU — çağrılmazsa splash view sonsuza kadar ekranda asılı kalır.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val zoomOut = ObjectAnimator.ofFloat(splashScreenView.iconView, "scaleX", 1f, 1.15f)
            val zoomOutY = ObjectAnimator.ofFloat(splashScreenView.iconView, "scaleY", 1f, 1.15f)
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view, "translationY", 0f, -splashScreenView.view.height * 0.08f
            )
            val fadeOut = ObjectAnimator.ofFloat(splashScreenView.view, "alpha", 1f, 0f)

            AnimatorSet().apply {
                playTogether(zoomOut, zoomOutY, slideUp, fadeOut)
                duration = 350L
                interpolator = AccelerateInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) = Unit
                    override fun onAnimationCancel(animation: Animator) = Unit
                    override fun onAnimationRepeat(animation: Animator) = Unit
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                start()
            }
        }

        setContent {
            // Daha önce burada çıplak MaterialTheme {} çağrılıyordu; UniTrackTheme hiç
            // bağlanmamıştı. Artık Material 3 + Dynamic Color + Dark Theme gerçekten aktif.
            //
            // Surface yerine AmbientBackground: tüm uygulamanın arkasında tek bir yerden
            // yönetilen premium koyu zemin + köşe ışıkları (bkz. AmbientBackground.kt).
            val themeMode by themeViewModel.themeMode.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            UniTrackTheme(darkTheme = darkTheme) {
                val isExamWeek by ambientMoodViewModel.isExamWeek.collectAsState()

                AmbientBackground(
                    modifier = Modifier.fillMaxSize(),
                    examWeek = isExamWeek
                ) {
                    val authState by authViewModel.authState.collectAsState()

                    when (val state = authState) {
                        is AuthState.Success -> {
                            UniTrackNavHost(
                                welcomeName = state.name,
                                onLogout = { authViewModel.logout() },
                                themeMode = themeMode,
                                onThemeModeChange = { themeViewModel.setThemeMode(it) }
                            )
                        }
                        AuthState.Checking -> {
                            // Bu dala normalde hiç düşülmez: splash screen bu state boyunca
                            // ekranı zaten kaplıyor. Yine de savunma amaçlı boş bırakılıyor.
                        }
                        else -> {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    // authState Success olduğunda yukarıdaki when otomatik panele geçer
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Widget'ın en son GANO/görev verisini göstermesi için 30 dakikalık sistem
    // periyodunu beklemek yerine, kullanıcı uygulamayı her açtığında (ör. yeni
    // görev eklendikten sonra) sessizce tazeliyoruz.
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            UniTrackWidget().updateAll(this@MainActivity)
        }
    }
}