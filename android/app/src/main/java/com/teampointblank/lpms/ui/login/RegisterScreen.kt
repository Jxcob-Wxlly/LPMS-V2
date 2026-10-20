package com.teampointblank.lpms.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.ui.components.DropdownField
import com.teampointblank.lpms.ui.components.FieldTextAction
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LpmsTextField
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onSignIn: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val palette = LpmsColors.palette
    val busy = state.isSaving

    AuthScaffold(title = "Create your account", subtitle = "For READ programme teachers", showLogo = true) {
        val formError = state.formError
        if (formError != null) InfoBanner(text = formError, isError = true)

        LpmsTextField(
            label = "Your name",
            value = state.fullName,
            onValueChange = viewModel::onNameChange,
            enabled = !busy,
            error = state.nameError,
            placeholder = "First and last name",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        LpmsTextField(
            label = "Email",
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            enabled = !busy,
            error = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        DropdownField(
            label = "School",
            options = state.schools,
            selected = state.school,
            optionLabel = { it.schoolName },
            onSelect = viewModel::onSchoolChange,
            placeholder = if (state.schoolsLoading) "Loading schools..." else "Choose your school",
            error = state.schoolError,
            enabled = !busy && state.schools.isNotEmpty()
        )
        if (!state.schoolsLoading && state.schools.isEmpty()) {
            Text(
                "Try again",
                modifier = Modifier.clickable(onClick = viewModel::loadSchools).padding(start = 4.dp),
                color = palette.accent,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
        LpmsTextField(
            label = "Password",
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            enabled = !busy,
            error = state.passwordError,
            hint = "At least 8 characters with an uppercase letter, a lowercase letter and a number.",
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                FieldTextAction(if (state.showPassword) "Hide" else "Show", viewModel::toggleShowPassword)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.submit()
            })
        )

        PrimaryButton(
            text = "Create account",
            loading = busy,
            onClick = {
                focusManager.clearFocus()
                viewModel.submit()
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already registered? ", color = palette.textMuted, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Sign in",
                modifier = Modifier.clickable(onClick = onSignIn),
                color = palette.accent,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
