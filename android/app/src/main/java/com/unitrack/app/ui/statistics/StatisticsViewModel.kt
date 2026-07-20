package com.unitrack.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.StatisticsDto
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    data class Success(val data: StatisticsDto) : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading

            runCatching {
                repository.getStatistics()
            }.onSuccess {
                _uiState.value = StatisticsUiState.Success(it)
            }.onFailure {
                _uiState.value = StatisticsUiState.Error(it.message ?: "İstatistikler yüklenemedi.")
            }
        }
    }

    /** Pull-to-refresh: mevcut veri ekranda kalır, sadece arka planda tazelenir. */
    fun refresh() {
        if (_uiState.value !is StatisticsUiState.Success) {
            load()
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true

            runCatching {
                repository.getStatistics()
            }.onSuccess {
                _uiState.value = StatisticsUiState.Success(it)
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
