package com.teampointblank.lpms.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.ui.components.FieldTextAction
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LpmsTextField
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.settings.ServerAddressDialog
import com.teampointblank.lpms.ui.settings.SettingsViewModel
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenHelp: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val palette = LpmsColors.palette
    var showServerDialog by remember { mutableStateOf(false) }

    AuthScaffold(title = "Welcome back", subtitle = "Sign in to LPMS", showLogo = false) {
        val notice = state.notice
        if (notice != null) InfoBanner(text = notice)
        val formError = state.formError
        if (formError != null) InfoBanner(text = formError, isError = true)

        LpmsTextField(
            label = "Email or username",
            value = state.identifier,
            onValueChange = viewModel::onIdentifierChange,
            enabled = !state.isLoading,
            error = state.identifierError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        LpmsTextField(
            label = "Password",
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            enabled = !state.isLoading,
            error = state.passwordError,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                FieldTextAction(if (state.showPassword) "Hide" else "Show", viewModel::toggleShowPassword)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.signIn()
            })
        )

        PrimaryButton(
            text = "Sign in",
            loading = state.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.signIn()
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New teacher? ", color = palette.textMuted, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Create an account",
                modifier = Modifier.clickable(onClick = onCreateAccount),
                color = palette.accent,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Help & FAQ",
                modifier = Modifier.clickable(onClick = onOpenHelp).padding(8.dp),
                color = palette.textMuted,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = TextDecoration.Underline
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Server settings",
                modifier = Modifier.clickable { showServerDialog = true }.padding(8.dp),
                color = palette.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                settings.serverUrl,
                color = palette.textMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showServerDialog) {
        ServerAddressDialog(viewModel = settingsViewModel, onDismiss = { showServerDialog = false })
    }
}
