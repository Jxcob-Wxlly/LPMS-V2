package com.teampointblank.lpms.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.AppSettings
import com.teampointblank.lpms.data.AuthManager
import com.teampointblank.lpms.data.AuthState
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SettingsStore
import com.teampointblank.lpms.data.ThemeMode
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerTestResult(val ok: Boolean, val message: String)

data class ServerUi(
    val input: String = "",
    val error: String? = null,
    val isTesting: Boolean = false,
    val result: ServerTestResult? = null
)

class SettingsViewModel(
    private val store: SettingsStore,
    private val repository: LpmsRepository,
    private val auth: AuthManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.state
    val authState: StateFlow<AuthState> = auth.state

    private val _server = MutableStateFlow(ServerUi(input = store.current.serverUrl))
    val server: StateFlow<ServerUi> = _server.asStateFlow()

    private var testJob: Job? = null

    fun setTheme(mode: ThemeMode) = store.setThemeMode(mode)

    fun setLargeText(enabled: Boolean) = store.setLargeText(enabled)

    fun setAttendanceTarget(percent: Int) = store.setAttendanceTarget(percent)

    fun setRememberUsername(enabled: Boolean) = store.setRememberUsername(enabled)

    fun onServerInput(value: String) {
        _server.update { it.copy(input = value.take(200), error = null, result = null) }
    }

    /** Validates the typed address, saves it, then calls /api/health to prove it works. */
    fun saveAndTestServer() {
        val check = Validators.normalizeServerUrl(_server.value.input)
        val url = check.value
        if (url == null) {
            _server.update { it.copy(error = check.error, result = null) }
            return
        }
        store.setServerUrl(url)
        _server.update { it.copy(input = url, error = null, isTesting = true, result = null) }
        runTest()
    }

    fun testServer() {
        _server.update { it.copy(error = null, isTesting = true, result = null) }
        runTest()
    }

    fun resetServer() {
        store.resetServerUrl()
        _server.update { it.copy(input = store.current.serverUrl, error = null, result = null) }
    }

    fun signOut() {
        repository.signOut()
    }

    private fun runTest() {
        testJob?.cancel()
        testJob = viewModelScope.launch {
            val outcome = when (val result = repository.checkServer()) {
                is ApiResult.Success -> {
                    val info = result.data
                    ServerTestResult(
                        ok = info.status.equals("Healthy", ignoreCase = true),
                        message = "Connected. Database: ${info.database.ifBlank { "unknown" }}. API version ${info.version.ifBlank { "unknown" }}."
                    )
                }
                is ApiResult.Failure -> ServerTestResult(ok = false, message = result.message)
            }
            _server.update { it.copy(isTesting = false, result = outcome) }
        }
    }
}
