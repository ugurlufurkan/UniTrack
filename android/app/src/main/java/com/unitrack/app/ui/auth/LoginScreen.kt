package com.unitrack.app.ui.auth

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unitrack.app.ui.components.GlassCard
import com.unitrack.app.ui.components.PressableButton
import com.unitrack.app.ui.components.click
import com.unitrack.app.ui.theme.Spacing
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

// BURASI ÇOK ÖNEMLİ: Kopyaladığın o uzun Web Client ID anahtarı
private const val WEB_CLIENT_ID = "402728610592-pp9smo93r7c8sqg3kdm29arhdbp22njp.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Madde #3 — Animated Gradient: giriş ekranının kendine özel,
            // yavaşça dönen bir renk yıkaması var. AmbientBackground zaten
            // TÜM uygulamanın arkasında duran genel köşe ışıklarını
            // sağlıyor (bkz. AmbientBackground.kt); bu, ona ek olarak
            // SADECE Login ekranına özel, daha belirgin bir "canlı" his —
            // kullanıcının uygulamayla ilk karşılaştığı an bu yüzden diğer
            // ekranlardan biraz daha "hareketli".
            .then(Modifier.loginGradientWash())
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LoginLogoBadge()

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "TRACK YOUR SUCCESS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(Spacing.hairline))

            Text(
                text = "UniTrack",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Ankara Bilim Üniversitesi Öğrenci Sistemi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    when (authState) {
                        is AuthState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is AuthState.Error -> {
                            val errorMessage = (authState as AuthState.Error).error
                            Text(text = "Hata: $errorMessage", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            GoogleLoginButton(viewModel)
                        }
                        else -> {
                            GoogleLoginButton(viewModel)
                        }
                    }

                }
            }

        }

    }
}

/**
 * Madde #28 — Ultra Premium Login: marka rozeti. Üstte çok bulanık, soluk bir
 * ışık huzmesi (gerçek `Modifier.blur`, API 31+'da çalışır — kendi çizdiğimiz
 * bir gradyan şekli bulandırıyoruz, bu her zaman güvenli/meşru bir kullanım,
 * bkz. AmbientBackground.kt'deki aynı gerekçe), altında cam görünümlü dairesel
 * bir rozet içinde ikon. Rozet hafifçe "nefes alıyor" (opaklık pulsasyonu) —
 * ekran statik durmasın diye.
 */
@Composable
private fun LoginLogoBadge() {
    val transition = rememberInfiniteTransition(label = "login-badge-breathe")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "login-badge-glow-alpha"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        // Üstte çok bulanık, geniş ışık huzmesi — rozetin arkasından taşıyor.
        Box(
            modifier = Modifier
                .size(180.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Cam rozet: GlassCard'la aynı dil (gradyan yüzey + üstten aydınlık
        // kenarlık) ama dairesel — GlassCard'ın kendisi köşeli kart için
        // tasarlandığından burada aynı görünümü elle kuruyoruz.
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * `Brush.linearGradient`'ın başlangıç/bitiş noktalarını her karede
 * bir çember üzerinde (cos/sin) kaydırarak "sabit bir gradyanın statik
 * durması" yerine sürekli yön değiştiren bir ışık hissi veriyoruz.
 * Gerçek bir rotasyon (graphicsLayer.rotationZ) kullanmadık çünkü o,
 * üstündeki tüm içeriği de döndürürdü; burada sadece zeminin gradyanı dönüyor.
 */
@Composable
private fun Modifier.loginGradientWash(): Modifier {
    val transition = rememberInfiniteTransition(label = "login-gradient")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "login-gradient-angle"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiaryOrSecondary = MaterialTheme.colorScheme.secondary

    return this.drawBehind {
        val radians = Math.toRadians(angle.toDouble())
        val radius = size.maxDimension * 0.75f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val start = Offset(
            x = centerX + radius * cos(radians).toFloat(),
            y = centerY + radius * sin(radians).toFloat()
        )
        val end = Offset(
            x = centerX - radius * cos(radians).toFloat(),
            y = centerY - radius * sin(radians).toFloat()
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.10f),
                    tertiaryOrSecondary.copy(alpha = 0.08f)
                ),
                start = start,
                end = end
            )
        )
    }
}

@Composable
fun GoogleLoginButton(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Önceden hata sadece Logcat'e yazılıp kullanıcıya hiçbir şey gösterilmiyordu —
    // bu yüzden "butona basınca hiçbir şey olmuyor" hissi oluşuyordu. Artık hatayı
    // burada tutup butonun altında okunabilir bir mesaj olarak gösteriyoruz.
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PressableButton(
            onClick = {
                haptic.click()
                errorMessage = null
                isLoading = true
                coroutineScope.launch {
                    val credentialManager = CredentialManager.create(context)

                    // Google penceresini hazırlıyoruz.
                    // setAutoSelectEnabled(true) + tek kayıtlı hesap olmadığı durumlarda
                    // bazı cihazlarda sessizce (hiç bottom-sheet açmadan) NoCredentialException
                    // fırlatabiliyordu. false yaparak her zaman hesap seçme ekranının
                    // açılmasını garantiliyoruz.
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(WEB_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    try {
                        // Google'ın alttan kayan hesap seçme ekranını aç
                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential

                        // Kullanıcı hesabı seçtiyse gerçek Token'ı alıp Backend'e yolla
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken

                            viewModel.loginWithGoogle(idToken) // Backend'e ateşle!
                        } else {
                            errorMessage = "Google hesabı alınamadı (beklenmeyen kimlik bilgisi türü)."
                        }
                    } catch (e: GetCredentialCancellationException) {
                        // Kullanıcı hesap seçme ekranını kendi kapattı — bu bir hata değil,
                        // sessizce geri dön.
                    } catch (e: NoCredentialException) {
                        errorMessage = "Bu cihazda/emülatörde kayıtlı bir Google hesabı bulunamadı. " +
                            "Ayarlar > Hesaplar'dan bir Google hesabı ekleyip tekrar dene."
                        Log.e("GOOGLE_AUTH", "No credential available", e)
                    } catch (e: GetCredentialException) {
                        errorMessage = "Google girişi başarısız: ${e.message ?: e.type}"
                        Log.e("GOOGLE_AUTH", "GetCredentialException", e)
                    } catch (e: Exception) {
                        errorMessage = "Beklenmeyen bir hata oluştu: ${e.message}"
                        Log.e("GOOGLE_AUTH", "Google Login Exception", e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.7f),
            contentPadding = PaddingValues(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Google ile Giriş Yap", style = MaterialTheme.typography.titleMedium)
            }
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
    }
}
