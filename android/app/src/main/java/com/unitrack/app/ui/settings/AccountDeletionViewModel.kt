package com.unitrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.local.OfflineCache
import com.unitrack.app.data.repository.AuthRepository
import com.unitrack.app.data.session.SessionEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AccountDeletionState {
    object Idle : AccountDeletionState()
    object Deleting : AccountDeletionState()
    object Error : AccountDeletionState()
    // Success sonrası ekranda bir şey göstermeye gerek yok: SessionEventBus
    // zaten AuthViewModel'i Idle'a çekip kullanıcıyı LoginScreen'e döndürüyor.
}

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val offlineCache: OfflineCache,
    private val sessionEventBus: SessionEventBus
) : ViewModel() {

    private val _state = MutableStateFlow<AccountDeletionState>(AccountDeletionState.Idle)
    val state: StateFlow<AccountDeletionState> = _state.asStateFlow()

    fun deleteAccount() {
        if (_state.value == AccountDeletionState.Deleting) return

        viewModelScope.launch {
            _state.value = AccountDeletionState.Deleting
            val success = runCatching { authRepository.deleteAccount() }.getOrDefault(false)

            if (success) {
                offlineCache.clear()
                sessionEventBus.notifyForceLogout()
                // authState burada Idle olacağı için MainActivity otomatik
                // LoginScreen'e döner; bu ViewModel'in state'i artık önemsiz.
            } else {
                _state.value = AccountDeletionState.Error
            }
        }
    }

    fun dismissError() {
        _state.value = AccountDeletionState.Idle
    }
}
