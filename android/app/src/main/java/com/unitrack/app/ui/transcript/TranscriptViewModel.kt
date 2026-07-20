package com.unitrack.app.ui.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.TranscriptEntryDto
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TranscriptUiState {
    object Loading : TranscriptUiState()
    data class Success(val entries: List<TranscriptEntryDto>) : TranscriptUiState()
    data class Error(val message: String) : TranscriptUiState()
}

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranscriptUiState>(TranscriptUiState.Loading)
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TranscriptUiState.Loading
            try {
                _uiState.value = TranscriptUiState.Success(repository.getTranscript())
            } catch (e: Exception) {
                _uiState.value = TranscriptUiState.Error(e.message ?: "Transkript yüklenemedi.")
            }
        }
    }

    /** Pull-to-refresh: mevcut liste ekranda kalır, sadece arka planda tazelenir. */
    fun refresh() {

        if (_uiState.value !is TranscriptUiState.Success) {
            load()
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _uiState.value = TranscriptUiState.Success(repository.getTranscript())
                _refreshError.value = null
            } catch (e: Exception) {
                _refreshError.value = e.message ?: "Tazelenemedi, eski veriler gösteriliyor."
            }
            _isRefreshing.value = false
        }

    }

    fun clearRefreshError() {
        _refreshError.value = null
    }
}
