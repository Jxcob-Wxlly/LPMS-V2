@file:OptIn(ExperimentalMaterial3Api::class)

package com.teampointblank.lpms.ui.learners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.Assessment
import com.teampointblank.lpms.data.LearnerAttendance
import com.teampointblank.lpms.data.LearnerProgress
import com.teampointblank.lpms.domain.AttendanceLogic
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.domain.ReportLogic
import com.teampointblank.lpms.ui.components.AccentChip
import com.teampointblank.lpms.ui.components.ConfirmDialog
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.HeaderBlock
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.LpmsSnackbarHost
import com.teampointblank.lpms.ui.components.LpmsTextField
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.components.SectionHeader
import com.teampointblank.lpms.ui.components.SmallAccentButton
import com.teampointblank.lpms.ui.components.StatTile
import com.teampointblank.lpms.ui.components.StatusChip
import com.teampointblank.lpms.ui.components.pullUp
import com.teampointblank.lpms.ui.report.exportAndShareReport
import com.teampointblank.lpms.ui.theme.LpmsColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun LearnerDetailScreen(viewModel: LearnerDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = LpmsColors.palette.background,
        snackbarHost = { LpmsSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val progress = state.progress
            when {
                progress == null && state.isLoading -> LoadingBox()
                progress == null -> ErrorState(
                    message = state.error ?: "Something went wrong. Please try again.",
                    onRetry = { viewModel.load() }
                )
                else -> ProgressContent(
                    progress = progress,
                    isRefreshing = state.isLoading,
                    error = state.error,
                    onBack = onBack,
                    onAdd = viewModel::openAdd,
                    onEdit = viewModel::openEdit,
                    onDelete = viewModel::askDelete,
                    onShare = {
                        scope.launch {
                            val error = exportAndShareReport(context, progress, ReportLogic.DEFAULT_TARGET)
                            if (error != null) snackbarHostState.showSnackbar(error)
                        }
                    }
                )
            }
        }
    }

    val form = state.form
    if (form != null) {
        AssessmentSheet(
            form = form,
            onScore = viewModel::onScoreChange,
            onDate = viewModel::onDateChange,
            onNotes = viewModel::onNotesChange,
            onSave = viewModel::saveForm,
            onDismiss = viewModel::closeForm
        )
    }

    val target = state.deleteTarget
    if (target != null) {
        ConfirmDialog(
            title = "Delete this assessment?",
            message = "The score of ${Formatters.score(target.assessmentScore)} recorded on " +
                "${Formatters.shortDate(target.assessmentDate)} will be removed. This can't be undone.",
            confirmLabel = if (state.isDeleting) "Deleting..." else "Delete",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }
}

@Composable
private fun ProgressContent(
    progress: LearnerProgress,
    isRefreshing: Boolean,
    error: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Assessment) -> Unit,
    onDelete: (Assessment) -> Unit,
    onShare: () -> Unit
) {
    val palette = LpmsColors.palette
    val learner = progress.learner

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeaderBlock(bottomPadding = 64.dp) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BackLink(onBack)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = "Download PDF report", tint = palette.onHeader)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                Formatters.fullName(learner.firstName, learner.lastName),
                color = palette.onHeader,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                "${learner.grade} · ${learner.schoolName}",
                color = palette.onHeaderMuted,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(Modifier.height(14.dp))
            AccentChip(progress.progressLabel.ifBlank { "No assessments yet" })
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.pullUp(44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Average score", Formatters.score(learner.averageScore), Modifier.weight(1f))
                StatTile("Attendance", Formatters.percent(learner.attendanceRate), Modifier.weight(1f))
            }
            if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
            if (error != null) InfoBanner(text = error, isError = true)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Assessments", modifier = Modifier.weight(1f))
                SmallAccentButton("Add", Icons.Filled.Add, onAdd)
            }
            if (progress.assessments.isEmpty()) {
                DashedEmpty("No assessments yet.")
            } else {
                progress.assessments.forEach { assessment ->
                    AssessmentCard(assessment, onEdit = { onEdit(assessment) }, onDelete = { onDelete(assessment) })
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Recent attendance")
            if (progress.attendance.isEmpty()) {
                Text(
                    "No attendance recorded.",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                progress.attendance.take(10).forEach { entry -> AttendanceRow(entry) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BackLink(onBack: () -> Unit) {
    val palette = LpmsColors.palette
    Row(
        modifier = Modifier
            .clickable(onClick = onBack)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = palette.onHeader,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text("Back", color = palette.onHeader, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AssessmentCard(assessment: Assessment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LpmsColors.palette
    LpmsCard(shape = RoundedCornerShape(32.dp)) {
        Row(
            modifier = Modifier.padding(start = 22.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Formatters.score(assessment.assessmentScore),
                modifier = Modifier.padding(end = 26.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.accent
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Formatters.weekdayDate(assessment.assessmentDate),
                    color = palette.text,
                    style = MaterialTheme.typography.bodyLarge
                )
                val notes = assessment.notes
                if (!notes.isNullOrBlank()) {
                    Text(notes, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Recorded by ${assessment.recordedBy}",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (assessment.canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit assessment", tint = palette.text)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete assessment", tint = palette.danger)
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(entry: LearnerAttendance) {
    val colors = LpmsColors.status
    val palette = LpmsColors.palette
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Formatters.date(entry.sessionDate), color = palette.text, style = MaterialTheme.typography.bodyLarge)
            Text(entry.programmeName, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (entry.status == AttendanceLogic.PRESENT) {
            StatusChip("Present", colors.successContainer, colors.onSuccessContainer)
        } else {
            StatusChip(
                entry.status.ifBlank { "Absent" },
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** Add / edit assessment as a bottom sheet (see the mockups). */
@Composable
private fun AssessmentSheet(
    form: AssessmentForm,
    onScore: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val palette = LpmsColors.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!form.isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.text,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (form.editingId == null) "Add assessment" else "Edit assessment",
                    modifier = Modifier.align(Alignment.Center),
                    color = palette.text,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = onDismiss,
                    enabled = !form.isSaving,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = palette.text)
                }
            }

            val formError = form.formError
            if (formError != null) InfoBanner(text = formError, isError = true)

            LpmsTextField(
                label = "Score (0–100)",
                value = form.score,
                onValueChange = onScore,
                placeholder = "e.g. 72.5",
                enabled = !form.isSaving,
                error = form.scoreError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                LpmsTextField(
                    label = "Date",
                    value = if (form.date == null) "" else Formatters.shortDate(form.date),
                    onValueChange = {},
                    readOnly = true,
                    error = form.dateError,
                    trailing = {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose date", tint = palette.text)
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = !form.isSaving) { showPicker = true }
                )
            }

            LpmsTextField(
                label = "Notes",
                value = form.notes,
                onValueChange = onNotes,
                enabled = !form.isSaving,
                singleLine = false,
                minLines = 3,
                error = form.notesError
            )
            Text(
                "${form.notes.length}/255",
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                color = palette.textMuted,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium
            )

            PrimaryButton(text = "Save", onClick = onSave, loading = form.isSaving)
        }
    }

    if (showPicker) {
        PastDatePickerDialog(
            initial = form.date,
            maxDate = Formatters.today(),
            onPicked = {
                onDate(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/** Date picker that doesn't allow dates after [maxDate]. */
@Composable
private fun PastDatePickerDialog(
    initial: LocalDate?,
    maxDate: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LpmsColors.palette
    val maxMillis = maxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
            override fun isSelectableYear(year: Int): Boolean = year <= maxDate.year
        }
    )
    val colors = DatePickerDefaults.colors(
        containerColor = palette.card,
        titleContentColor = palette.textMuted,
        headlineContentColor = palette.text,
        weekdayContentColor = palette.textMuted,
        subheadContentColor = palette.textMuted,
        navigationContentColor = palette.text,
        yearContentColor = palette.text,
        currentYearContentColor = palette.accent,
        selectedYearContainerColor = palette.accent,
        selectedYearContentColor = palette.onAccent,
        dayContentColor = palette.text,
        disabledDayContentColor = palette.textMuted.copy(alpha = 0.4f),
        selectedDayContainerColor = palette.accent,
        selectedDayContentColor = palette.onAccent,
        todayContentColor = palette.accent,
        todayDateBorderColor = palette.accent
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null
            ) { Text("OK", color = palette.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textMuted) } }
    ) {
        DatePicker(state = pickerState, colors = colors)
    }
}
