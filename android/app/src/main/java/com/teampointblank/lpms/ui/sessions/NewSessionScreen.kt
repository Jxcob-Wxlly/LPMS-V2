package com.teampointblank.lpms.ui.sessions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.DropdownField
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsDatePickerDialog
import com.teampointblank.lpms.ui.components.LpmsTimePickerDialog
import com.teampointblank.lpms.ui.components.LpmsTopBar
import com.teampointblank.lpms.ui.components.PickerField
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.theme.LpmsColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val clock = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun NewSessionScreen(viewModel: NewSessionViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) {
            Toast.makeText(context, "Session scheduled.", Toast.LENGTH_SHORT).show()
            viewModel.consumeDone()
            onBack()
        }
    }

    Scaffold(
        containerColor = LpmsColors.palette.background,
        topBar = { LpmsTopBar(title = "New session", onBack = onBack) }
    ) { padding ->
        val loadError = state.loadError
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            loadError != null -> ErrorState(loadError, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Schedule a session at your school. It shows up under Upcoming as soon as you save.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LpmsColors.palette.textMuted
                )
                val formError = state.formError
                if (formError != null) InfoBanner(text = formError, isError = true)

                DropdownField(
                    label = "Programme",
                    options = state.programmes,
                    selected = state.programme,
                    optionLabel = { it.programmeName },
                    onSelect = viewModel::onProgramme,
                    placeholder = "Choose a programme",
                    error = state.programmeError,
                    enabled = !state.isSaving
                )
                PickerField(
                    label = "Date",
                    text = state.date?.let { Formatters.date(it.toString()) } ?: "",
                    placeholder = "Choose the date",
                    error = state.dateError,
                    enabled = !state.isSaving,
                    onClick = { showDate = true }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PickerField(
                        label = "Starts",
                        text = state.start?.format(clock) ?: "",
                        placeholder = "09:00",
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                        onClick = { showStart = true }
                    )
                    PickerField(
                        label = "Ends",
                        text = state.end?.format(clock) ?: "",
                        placeholder = "10:00",
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                        onClick = { showEnd = true }
                    )
                }
                val timeError = state.timeError
                if (timeError != null) {
                    Text(timeError, color = LpmsColors.palette.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton(text = "Save session", loading = state.isSaving, onClick = viewModel::save)
            }
        }
    }

    if (showDate) {
        LpmsDatePickerDialog(
            initial = state.date,
            minDate = state.programmeStart?.takeIf { it.isAfter(Formatters.today()) } ?: Formatters.today(),
            maxDate = state.programmeEnd,
            onPicked = {
                viewModel.onDate(it)
                showDate = false
            },
            onDismiss = { showDate = false }
        )
    }
    if (showStart) {
        LpmsTimePickerDialog(
            title = "Start time",
            initial = state.start,
            onPicked = {
                viewModel.onStart(it)
                showStart = false
            },
            onDismiss = { showStart = false }
        )
    }
    if (showEnd) {
        LpmsTimePickerDialog(
            title = "End time",
            initial = state.end ?: state.start?.plusHours(1) ?: LocalTime.of(10, 0),
            onPicked = {
                viewModel.onEnd(it)
                showEnd = false
            },
            onDismiss = { showEnd = false }
        )
    }
}
