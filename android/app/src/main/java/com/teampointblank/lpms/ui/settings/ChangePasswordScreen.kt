package com.teampointblank.lpms.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.ui.components.FieldTextAction
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LpmsTextField
import com.teampointblank.lpms.ui.components.LpmsTopBar
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.done) {
        if (state.done) {
            Toast.makeText(context, "Password changed.", Toast.LENGTH_SHORT).show()
            viewModel.consumeDone()
            onBack()
        }
    }

    val transformation = if (state.showPasswords) VisualTransformation.None else PasswordVisualTransformation()
    val showToggle: @Composable () -> Unit = {
        FieldTextAction(if (state.showPasswords) "Hide" else "Show", viewModel::toggleShow)
    }

    Scaffold(
        containerColor = LpmsColors.palette.background,
        topBar = { LpmsTopBar(title = "Change password", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Use at least 8 characters with an uppercase letter, a lowercase letter and a number.",
                style = MaterialTheme.typography.bodyLarge,
                color = LpmsColors.palette.textMuted
            )
            val formError = state.formError
            if (formError != null) InfoBanner(text = formError, isError = true)

            LpmsTextField(
                label = "Current password",
                value = state.current,
                onValueChange = viewModel::onCurrentChange,
                enabled = !state.isSaving,
                error = state.currentError,
                visualTransformation = transformation,
                trailing = showToggle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            LpmsTextField(
                label = "New password",
                value = state.newPassword,
                onValueChange = viewModel::onNewChange,
                enabled = !state.isSaving,
                error = state.newError,
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            LpmsTextField(
                label = "Confirm new password",
                value = state.confirm,
                onValueChange = viewModel::onConfirmChange,
                enabled = !state.isSaving,
                error = state.confirmError,
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.submit()
                })
            )
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Change password",
                loading = state.isSaving,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.submit()
                }
            )
        }
    }
}
