package com.unitrack.app.ui.gpa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.GpaDto
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GpaUiState {
    object Loading : GpaUiState()
    data class Success(val data: GpaDto) : GpaUiState()
    data class Error(val message: String) : GpaUiState()
}

@HiltViewModel
class GpaViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<GpaUiState>(GpaUiState.Loading)

    val uiState: StateFlow<GpaUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {

            _uiState.value = GpaUiState.Loading

            runCatching {
                repository.getGpa()
            }.onSuccess {
                _uiState.value = GpaUiState.Success(it)
            }.onFailure {
                _uiState.value =
                    GpaUiState.Error(it.message ?: "GPA yüklenemedi.")
            }
        }
    }

    /** Pull-to-refresh: mevcut veri ekranda kalır, sadece arka planda tazelenir. */
    fun refresh() {

        if (_uiState.value !is GpaUiState.Success) {
            load()
            return
        }

        viewModelScope.launch {

            _isRefreshing.value = true

            runCatching {
                repository.getGpa()
            }.onSuccess {
                _uiState.value = GpaUiState.Success(it)
                _refreshError.value = null
            }.onFailure {
                _refreshError.value = it.message ?: "Tazelenemedi, eski veriler gösteriliyor."
            }

            _isRefreshing.value = false

        }

    }

    fun clearRefreshError() {
        _refreshError.value = null
    }
}
