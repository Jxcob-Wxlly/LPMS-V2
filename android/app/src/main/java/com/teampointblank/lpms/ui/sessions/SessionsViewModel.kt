package com.teampointblank.lpms.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SessionSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionScope(val apiValue: String, val label: String) {
    Upcoming("upcoming", "Upcoming"),
    Past("past", "Past"),
    All("all", "All")
}

data class SessionsUiState(
    val scope: SessionScope = SessionScope.Upcoming,
    val sessions: List<SessionSummary>? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class SessionsViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun setScope(scope: SessionScope) {
        if (scope == _state.value.scope) return
        _state.update { it.copy(scope = scope, sessions = null) }
        load()
    }

    fun load() {
        job?.cancel()
        val scope = _state.value.scope
        _state.update { it.copy(isLoading = true, error = null) }
        job = viewModelScope.launch {
            when (val result = repository.sessions(scope.apiValue)) {
                is ApiResult.Success ->
                    _state.update { it.copy(sessions = result.data, isLoading = false, error = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
