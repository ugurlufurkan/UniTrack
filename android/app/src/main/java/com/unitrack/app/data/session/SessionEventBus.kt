package com.unitrack.app.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenAuthenticator, OkHttp'nin arka plan thread'inde çalışır ve Compose/ViewModel
 * katmanına doğrudan erişimi yoktur. Bir oturumu zorla sonlandırdığında (refresh
 * token süresi doldu, ya da backend reuse tespit edip tüm oturumları düşürdü)
 * bunu SADECE DataStore'a `clearTokens()` yazarak yapıyordu — ama hiçbir ekran bunu
 * dinlemiyordu. Sonuç: kullanıcı, arkada oturumu düşmüş bir ekranda takılı kalıyor,
 * "tekrar dene" hep 401 alıyor, LoginScreen'e asla dönmüyordu.
 *
 * Bu event bus, TokenAuthenticator'ın "artık oturum bitti" sinyalini AuthViewModel'e
 * iletmesini sağlıyor; AuthViewModel bunu dinleyip authState'i Idle'a çekiyor,
 * MainActivity'deki mevcut `when(authState)` de otomatik olarak LoginScreen'e döner.
 */
@Singleton
class SessionEventBus @Inject constructor() {

    // extraBufferCapacity = 1: emit eden taraf (Authenticator, arka plan thread'i)
    // dinleyen henüz collect etmeye başlamamışsa bile event kaybolmasın.
    private val _forceLogout = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogout: SharedFlow<Unit> = _forceLogout.asSharedFlow()

    /** TokenAuthenticator, kurtarılamaz bir oturum kaybı tespit ettiğinde çağırır. */
    fun notifyForceLogout() {
        _forceLogout.tryEmit(Unit)
    }
}
