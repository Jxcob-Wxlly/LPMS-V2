package com.teampointblank.lpms.data

import android.content.Context
import com.teampointblank.lpms.BuildConfig
import com.teampointblank.lpms.domain.ReportLogic
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode { System, Light, Dark }

/** Preferences chosen on the Settings screen. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val largeText: Boolean = false,
    val rememberUsername: Boolean = true,
    val savedUsername: String = "",
    val serverUrl: String = SettingsStore.defaultServerUrl(),
    /** Parent's expectation for a child's attendance, in percent. */
    val attendanceTarget: Int = ReportLogic.DEFAULT_TARGET
)

/** Stores app settings in private SharedPreferences and publishes them as a StateFlow. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val current: AppSettings get() = _state.value

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _state.update { it.copy(themeMode = mode) }
    }

    fun setLargeText(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply()
        _state.update { it.copy(largeText = enabled) }
    }

    fun setRememberUsername(enabled: Boolean) {
        val editor = prefs.edit().putBoolean(KEY_REMEMBER, enabled)
        if (!enabled) editor.putString(KEY_USERNAME, "")
        editor.apply()
        _state.update { it.copy(rememberUsername = enabled, savedUsername = if (enabled) it.savedUsername else "") }
    }

    fun saveUsername(username: String) {
        val value = if (current.rememberUsername) username else ""
        prefs.edit().putString(KEY_USERNAME, value).apply()
        _state.update { it.copy(savedUsername = value) }
    }

    /** [url] must already be normalised with [Validators.normalizeServerUrl]. */
    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
        _state.update { it.copy(serverUrl = url) }
    }

    fun setAttendanceTarget(percent: Int) {
        val value = ReportLogic.clampTarget(percent)
        prefs.edit().putInt(KEY_TARGET, value).apply()
        _state.update { it.copy(attendanceTarget = value) }
    }

    fun resetServerUrl() = setServerUrl(defaultServerUrl())

    private fun read(): AppSettings {
        val theme = ThemeMode.values().firstOrNull { it.name == prefs.getString(KEY_THEME, null) }
            ?: ThemeMode.System
        val storedUrl = prefs.getString(KEY_SERVER_URL, null)
        val url = if (storedUrl != null && Validators.normalizeServerUrl(storedUrl).value != null) {
            storedUrl
        } else {
            defaultServerUrl()
        }
        return AppSettings(
            themeMode = theme,
            largeText = prefs.getBoolean(KEY_LARGE_TEXT, false),
            rememberUsername = prefs.getBoolean(KEY_REMEMBER, true),
            savedUsername = prefs.getString(KEY_USERNAME, "") ?: "",
            serverUrl = url,
            attendanceTarget = ReportLogic.clampTarget(prefs.getInt(KEY_TARGET, ReportLogic.DEFAULT_TARGET))
        )
    }

    companion object {
        private const val PREFS_NAME = "lpms_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LARGE_TEXT = "large_text"
        private const val KEY_REMEMBER = "remember_username"
        private const val KEY_USERNAME = "saved_username"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TARGET = "attendance_target"

        /** The address compiled into the app (gradle.properties: lpmsApiBaseUrl). */
        fun defaultServerUrl(): String =
            Validators.normalizeServerUrl(BuildConfig.DEFAULT_API_URL).value ?: "https://lpms-api.azurewebsites.net/"
    }
}
