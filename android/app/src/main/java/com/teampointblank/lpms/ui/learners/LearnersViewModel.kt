package com.teampointblank.lpms.ui.learners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LearnerSummary
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnersUiState(
    val query: String = "",
    val learners: List<LearnerSummary>? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class LearnersViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(LearnersUiState())
    val state: StateFlow<LearnersUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Called as the teacher types; waits for a short pause before asking the server. */
    fun onQueryChange(value: String) {
        val query = value.take(Validators.MAX_SEARCH_LENGTH)
        _state.update { it.copy(query = query) }
        job?.cancel()
        job = viewModelScope.launch {
            delay(350)
            fetch(query)
        }
    }

    fun clearQuery() {
        _state.update { it.copy(query = "") }
        load()
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch { fetch(_state.value.query) }
    }

    private suspend fun fetch(query: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        val search = query.trim().ifEmpty { null }
        when (val result = repository.learners(search)) {
            is ApiResult.Success ->
                _state.update { it.copy(learners = result.data, isLoading = false, error = null) }
            is ApiResult.Failure ->
                _state.update { it.copy(isLoading = false, error = result.message) }
        }
    }
}
