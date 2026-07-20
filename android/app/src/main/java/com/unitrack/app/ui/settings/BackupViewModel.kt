package com.unitrack.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.repository.DataExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupExportState {
    object Idle : BackupExportState()
    object Loading : BackupExportState()
    data class Ready(val uri: Uri) : BackupExportState()
    data class Error(val message: String) : BackupExportState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: DataExportRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BackupExportState>(BackupExportState.Idle)
    val state: StateFlow<BackupExportState> = _state.asStateFlow()

    fun exportData() {
        // Zaten sürüyorsa tekrar tetiklenmesin (çift tıklama).
        if (_state.value is BackupExportState.Loading) return

        viewModelScope.launch {
            _state.value = BackupExportState.Loading
            _state.value = try {
                BackupExportState.Ready(repository.downloadBackup())
            } catch (e: Exception) {
                BackupExportState.Error(e.localizedMessage ?: "Yedek indirilemedi.")
            }
        }
    }

    fun consumeState() {
        _state.value = BackupExportState.Idle
    }
}
