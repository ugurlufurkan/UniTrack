package com.unitrack.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.local.OfflineCache
import com.unitrack.app.data.repository.AuthPreferences
import com.unitrack.app.data.repository.AuthRepository
import com.unitrack.app.data.repository.SettingsSyncRepository
import com.unitrack.app.data.session.SessionEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException // HATA KODLARINI YAKALAMAK İÇİN ŞART
import javax.inject.Inject

sealed class AuthState {
    // Uygulama açılışında kayıtlı oturum kontrol edilirken (splash screen bu sırada gösterilir).
    // Idle'dan bilerek ayrıldı: Idle "kontrol bitti, oturum yok" anlamına gelirken,
    // Checking "kontrol henüz sürüyor" anlamına gelir. Öncesinde ikisi karışmıştı ve
    // giriş yapmış bir kullanıcı bile açılışta bir anlığına LoginScreen görüyordu.
    object Checking : AuthState()
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val name: String, val message: String = "") : AuthState()
    data class Error(val error: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val authPreferences: AuthPreferences,
    private val offlineCache: OfflineCache,
    private val sessionEventBus: SessionEventBus,
    private val settingsSyncRepository: SettingsSyncRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Uygulama her açıldığında telefonda kayıtlı token var mı diye bak.
        // Varsa kullanıcıyı tekrar Google'a giriş yaptırmadan direkt içeri al.
        restoreSession()
        observeForceLogout()
    }

    /**
     * TokenAuthenticator, arka planda (ör. dashboard açıkken) oturumu kurtarılamaz
     * şekilde kaybettiğinde (refresh token süresi doldu / reuse tespit edildi) bunu
     * buradan öğreniyoruz. Aksi halde kullanıcı, authState hâlâ Success derken
     * arkada token'ları silinmiş bir ekranda "tekrar dene"nin hiç işe yaramadığı
     * bir durumda takılı kalırdı.
     */
    private fun observeForceLogout() {
        viewModelScope.launch {
            sessionEventBus.forceLogout.collect {
                offlineCache.clear()
                _authState.value = AuthState.Idle
            }
        }
    }

    /**
     * Sunucudaki tema/hedef GANO/sınav haftası ayarlarını yerel DataStore'lara
     * çeker — böylece kullanıcı yeni bir telefonda (veya uygulamayı silip
     * yeniden kurduğunda) aynı hesapla girince bu kişisel ayarlar da geri
     * gelir. Başarısız olursa (ör. internet yok) sessizce yutulur; ekranlar
     * zaten yerel varsayılanlarla çalışmaya devam eder, bir sonraki
     * fırsatta tekrar denenir.
     */
    private fun syncSettingsBestEffort() {
        viewModelScope.launch {
            runCatching { settingsSyncRepository.syncFromServer() }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val token = authPreferences.accessToken.first()
            val name = authPreferences.userName.first()

            _authState.value = if (!token.isNullOrBlank()) {
                syncSettingsBestEffort()
                AuthState.Success(name = name ?: "Öğrenci")
            } else {
                // Kontrol bitti, kayıtlı oturum yok -> artık net biçimde Idle'a geçiyoruz.
                AuthState.Idle
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.loginWithGoogle(idToken)

                if (response != null && response.success) {
                    authPreferences.saveTokens(
                        access = response.accessToken,
                        refresh = response.refreshToken,
                        name = response.user.name
                    )
                    _authState.value = AuthState.Success(
                        name = response.user.name ?: "Öğrenci",
                        message = "Giriş başarılı."
                    )
                    syncSettingsBestEffort()
                } else {
                    // Backend başarıyla cevap verdi ama success=false döndü
                    _authState.value = AuthState.Error(response?.message ?: "Backend girişi reddetti.")
                }
            } catch (e: HttpException) {
                // Backend 400, 401, 500 gibi hata kodları yolladığında buraya düşer
                val errorBody = e.response()?.errorBody()?.string()
                _authState.value = AuthState.Error("Sunucu Hatası: ${errorBody ?: "Erişim reddedildi"}")
            } catch (e: Exception) {
                // İnternet yoksa veya backend komple kapalıysa
                _authState.value = AuthState.Error("Bağlantı hatası: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val refreshToken = authPreferences.refreshToken.first().orEmpty()
                repository.logout(refreshToken)
            } finally {
                authPreferences.clearTokens()
                // Bir sonraki kullanıcı bu cihazda giriş yaparsa önceki
                // kullanıcının önbelleğe alınmış dashboard/ders verilerini
                // görmesin.
                offlineCache.clear()
                _authState.value = AuthState.Idle
            }
        }
    }
}