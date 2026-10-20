package com.teampointblank.lpms.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.TeacherDashboard
import com.teampointblank.lpms.ui.components.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(Resource<TeacherDashboard>(isLoading = true))
    val state: StateFlow<Resource<TeacherDashboard>> = _state.asStateFlow()

    private var job: Job? = null

    fun load() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        job = viewModelScope.launch {
            when (val result = repository.dashboard()) {
                is ApiResult.Success -> _state.value = Resource(data = result.data)
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
