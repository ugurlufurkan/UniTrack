package com.unitrack.app.ui.dashboard

import com.unitrack.app.data.dto.DashboardDto

sealed class DashboardUiState {

    object Loading : DashboardUiState()

    data class Success(
        val data: DashboardDto
    ) : DashboardUiState()

    data class Error(
        val message: String
    ) : DashboardUiState()
}