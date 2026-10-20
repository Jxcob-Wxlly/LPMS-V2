package com.teampointblank.lpms.ui.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.RosterEntry
import com.teampointblank.lpms.domain.AttendanceLogic
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.ConfirmDialog
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.LpmsSnackbarHost
import com.teampointblank.lpms.ui.components.LpmsTopBar
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.components.SessionStatusChip
import com.teampointblank.lpms.ui.components.TonalButton
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val palette = LpmsColors.palette

    val detail = state.detail
    val roster = detail?.learners ?: emptyList()
    val hasChanges = detail != null && AttendanceLogic.hasChanges(roster, state.draft)

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = hasChanges) { confirmDiscard = true }

    val canRecord = detail != null &&
        AttendanceLogic.canRecord(detail.session.sessionDate, Formatters.today())

    Scaffold(
        containerColor = palette.background,
        topBar = {
            LpmsTopBar(
                title = "Attendance",
                onBack = { if (hasChanges) confirmDiscard = true else onBack() },
                actions = {
                    IconButton(onClick = { viewModel.load() }, enabled = !state.isSaving) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Reload", tint = palette.onHeader)
                    }
                }
            )
        },
        snackbarHost = { LpmsSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (detail != null && roster.isNotEmpty() && canRecord) {
                SaveBar(
                    error = state.saveError,
                    isSaving = state.isSaving,
                    enabled = hasChanges && !state.isSaving,
                    onSave = { viewModel.save() }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                detail == null && state.isLoading -> LoadingBox()
                detail == null -> ErrorState(
                    message = state.loadError ?: "Something went wrong. Please try again.",
                    onRetry = { viewModel.load() }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
                            if (state.loadError != null) {
                                InfoBanner(text = state.loadError ?: "", isError = true)
                            }
                            SessionHeader(
                                detail.session.programmeName, detail.session.schoolName,
                                detail.session.sessionDate, detail.session.startTime,
                                detail.session.endTime, detail.session.status
                            )
                            if (!canRecord) {
                                InfoBanner(
                                    text = "This session hasn't happened yet. You can take attendance on the day of the session."
                                )
                            }
                        }
                    }
                    if (roster.isEmpty()) {
                        item { DashedEmpty("There are no learners registered at this school yet.") }
                    } else {
                        item { SummaryRow(roster, state.draft, canRecord, onMarkAll = { viewModel.markAll(AttendanceLogic.PRESENT) }) }
                        items(roster, key = { it.learnerId }) { learner ->
                            RosterRow(
                                learner = learner,
                                status = state.draft[learner.learnerId],
                                enabled = canRecord && !state.isSaving,
                                onSelect = { viewModel.setStatus(learner.learnerId, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            message = "You have attendance marks that haven't been saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
}

@Composable
private fun SessionHeader(
    programme: String,
    school: String,
    date: String,
    start: String,
    end: String,
    status: String
) {
    val palette = LpmsColors.palette
    LpmsCard {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text(programme, color = palette.text, style = MaterialTheme.typography.titleLarge)
            Text(school, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${Formatters.date(date)}, ${Formatters.timeRange(start, end)}",
                color = palette.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))
            SessionStatusChip(status)
        }
    }
}

@Composable
private fun SummaryRow(
    roster: List<RosterEntry>,
    draft: Map<Int, String>,
    canRecord: Boolean,
    onMarkAll: () -> Unit
) {
    val summary = AttendanceLogic.summary(roster, draft)
    val palette = LpmsColors.palette
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Present ${summary.present}  ·  Absent ${summary.absent}  ·  Not marked ${summary.notMarked}",
            color = palette.text,
            style = MaterialTheme.typography.titleSmall
        )
        TonalButton("All present", onClick = onMarkAll, enabled = canRecord)
    }
}

@Composable
private fun RosterRow(
    learner: RosterEntry,
    status: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val colors = LpmsColors.status
    val palette = LpmsColors.palette
    LpmsCard(shape = RoundedCornerShape(28.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Formatters.fullName(learner.firstName, learner.lastName),
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(learner.grade, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkPill(
                    label = "Present",
                    selected = status == AttendanceLogic.PRESENT,
                    selectedContainer = colors.successContainer,
                    selectedContent = colors.onSuccessContainer,
                    enabled = enabled,
                    onClick = { onSelect(AttendanceLogic.PRESENT) }
                )
                MarkPill(
                    label = "Absent",
                    selected = status == AttendanceLogic.ABSENT,
                    selectedContainer = MaterialTheme.colorScheme.errorContainer,
                    selectedContent = MaterialTheme.colorScheme.onErrorContainer,
                    enabled = enabled,
                    onClick = { onSelect(AttendanceLogic.ABSENT) }
                )
            }
        }
    }
}

@Composable
private fun MarkPill(
    label: String,
    selected: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = LpmsColors.palette
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (selected) selectedContainer else palette.chip,
        contentColor = if (selected) selectedContent else palette.textMuted
    ) {
        Text(
            if (selected) "✓ $label" else label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SaveBar(error: String?, isSaving: Boolean, enabled: Boolean, onSave: () -> Unit) {
    val palette = LpmsColors.palette
    Surface(shadowElevation = 8.dp, color = palette.nav) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (error != null) {
                Text(error, color = palette.danger, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(
                text = "Save attendance",
                onClick = onSave,
                enabled = enabled,
                loading = isSaving
            )
        }
    }
}
