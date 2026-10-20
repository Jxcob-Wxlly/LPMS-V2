@file:OptIn(ExperimentalMaterial3Api::class)

package com.teampointblank.lpms.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.BuildConfig
import com.teampointblank.lpms.data.SettingsStore
import com.teampointblank.lpms.data.ThemeMode
import com.teampointblank.lpms.domain.ReportLogic
import com.teampointblank.lpms.ui.components.ConfirmDialog
import com.teampointblank.lpms.ui.components.GroupDivider
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LpmsTextField
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.PillChip
import com.teampointblank.lpms.ui.components.ScreenTitle
import com.teampointblank.lpms.ui.components.SectionLabel
import com.teampointblank.lpms.ui.components.SettingsGroup
import com.teampointblank.lpms.ui.components.SettingsRow
import com.teampointblank.lpms.ui.components.SettingsToggleRow
import com.teampointblank.lpms.ui.components.TonalButton
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.theme.LpmsColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit,
    onChangePassword: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    val palette = LpmsColors.palette

    MainScaffold(currentRoute = Routes.SETTINGS, onNavigate = onNavigate, isParent = authState.isParent) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenTitle("Settings")
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionLabel("Appearance")
                SettingsGroup {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PillChip("Phone", settings.themeMode == ThemeMode.System, { viewModel.setTheme(ThemeMode.System) }, Modifier.weight(1f))
                        PillChip("Light", settings.themeMode == ThemeMode.Light, { viewModel.setTheme(ThemeMode.Light) }, Modifier.weight(1f))
                        PillChip("Dark", settings.themeMode == ThemeMode.Dark, { viewModel.setTheme(ThemeMode.Dark) }, Modifier.weight(1f))
                    }
                    GroupDivider()
                    SettingsToggleRow("Larger text", settings.largeText, viewModel::setLargeText)
                    GroupDivider()
                    SettingsToggleRow("Remember my email", settings.rememberUsername, viewModel::setRememberUsername)
                }

                if (authState.isParent) {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Attendance expectation")
                    SettingsGroup {
                        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                            Text(
                                "Your child's attendance is compared with this target on the children list, the report and the PDF.",
                                color = palette.textMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${settings.attendanceTarget}%",
                                color = palette.text,
                                style = MaterialTheme.typography.displaySmall
                            )
                            Slider(
                                value = settings.attendanceTarget.toFloat(),
                                onValueChange = { viewModel.setAttendanceTarget(it.roundToInt()) },
                                valueRange = ReportLogic.MIN_TARGET.toFloat()..ReportLogic.MAX_TARGET.toFloat(),
                                steps = (ReportLogic.MAX_TARGET - ReportLogic.MIN_TARGET) / 5 - 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.selected,
                                    activeTrackColor = palette.selected,
                                    inactiveTrackColor = palette.chip,
                                    activeTickColor = palette.onSelected,
                                    inactiveTickColor = palette.textMuted
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionLabel("Connection")
                SettingsGroup {
                    SettingsRow(
                        label = "Server",
                        value = serverName(settings.serverUrl),
                        onClick = { showServerDialog = true }
                    )
                    GroupDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonalButton(
                            text = if (server.isTesting) "Testing..." else "Test connection",
                            onClick = viewModel::testServer,
                            enabled = !server.isTesting
                        )
                        val result = server.result
                        if (result != null) {
                            Text(
                                result.message,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = if (result.ok) palette.textMuted else palette.danger,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionLabel("Account")
                SettingsGroup {
                    val profile = authState.profile
                    SettingsRow("Name", value = profile?.fullName.orEmpty())
                    GroupDivider()
                    SettingsRow("Email", value = profile?.email.orEmpty())
                    val school = profile?.schoolName
                    if (!school.isNullOrBlank()) {
                        GroupDivider()
                        SettingsRow("School", value = school)
                    }
                    GroupDivider()
                    SettingsRow(
                        "Change password",
                        onClick = onChangePassword,
                        trailing = { Chevron() }
                    )
                    GroupDivider()
                    SettingsRow(
                        "Help & FAQ",
                        onClick = onOpenHelp,
                        trailing = { Chevron() }
                    )
                    GroupDivider()
                    SettingsRow(
                        "Sign out",
                        onClick = { confirmSignOut = true },
                        labelColor = palette.danger
                    )
                }

                Text(
                    "LPMS v${BuildConfig.VERSION_NAME} · Built for READ Educational Trust",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You will need to sign in again to use the app.",
            confirmLabel = "Sign out",
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false }
        )
    }
    if (showServerDialog) {
        ServerAddressDialog(viewModel = viewModel, onDismiss = { showServerDialog = false })
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = LpmsColors.palette.text,
        modifier = Modifier.size(26.dp)
    )
}

/** "LPMS Cloud" for the built-in address, otherwise the host the user typed. */
private fun serverName(url: String): String {
    if (url == SettingsStore.defaultServerUrl()) return "LPMS Cloud"
    return url.removePrefix("https://").removePrefix("http://").trimEnd('/')
}

/** Dialog used on both the sign-in screen and Settings to change which server the app talks to. */
@Composable
fun ServerAddressDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val server by viewModel.server.collectAsStateWithLifecycle()
    val palette = LpmsColors.palette

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.text,
        textContentColor = palette.textMuted,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Server address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The web address of the LPMS API, for example https://lpms-api.azurewebsites.net/",
                    style = MaterialTheme.typography.bodySmall
                )
                LpmsTextField(
                    label = "Address",
                    value = server.input,
                    onValueChange = viewModel::onServerInput,
                    error = server.error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                )
                if (server.isTesting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.accent)
                        Spacer(Modifier.size(10.dp))
                        Text("Testing connection...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                val result = server.result
                if (result != null) {
                    InfoBanner(text = result.message, isError = !result.ok)
                }
                TextButton(onClick = viewModel::resetServer) {
                    Text("Reset to default", color = palette.textMuted)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveAndTestServer, enabled = !server.isTesting) {
                Text("Save and test", color = if (palette.isDark) palette.accent else palette.selected, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = palette.textMuted) } }
    )
}
