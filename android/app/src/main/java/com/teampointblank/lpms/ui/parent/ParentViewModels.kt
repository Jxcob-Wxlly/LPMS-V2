package com.teampointblank.lpms.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LearnerProgress
import com.teampointblank.lpms.data.LearnerSummary
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentHomeUiState(
    val children: List<LearnerSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ParentHomeViewModel(
    private val repository: LpmsRepository,
    settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(ParentHomeUiState())
    val state: StateFlow<ParentHomeUiState> = _state.asStateFlow()

    /** The parent's attendance expectation (percent), kept in sync with Settings. */
    val target: StateFlow<Int> = settings.state
        .map { it.attendanceTarget }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.attendanceTarget)

    private var job: Job? = null

    init {
        load()
    }

    fun load() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        job = viewModelScope.launch {
            when (val result = repository.children()) {
                is ApiResult.Success -> _state.update { ParentHomeUiState(result.data, false, null) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}

data class ChildReportUiState(
    val progress: LearnerProgress? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isExporting: Boolean = false,
    val message: String? = null
)

class ChildReportViewModel(
    private val repository: LpmsRepository,
    private val learnerId: Int,
    settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(ChildReportUiState())
    val state: StateFlow<ChildReportUiState> = _state.asStateFlow()

    val target: StateFlow<Int> = settings.state
        .map { it.attendanceTarget }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.attendanceTarget)

    private var job: Job? = null

    init {
        load()
    }

    fun load() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        job = viewModelScope.launch {
            when (val result = repository.learnerProgress(learnerId)) {
                is ApiResult.Success ->
                    _state.update { it.copy(progress = result.data, isLoading = false, error = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun setExporting(value: Boolean) = _state.update { it.copy(isExporting = value) }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
