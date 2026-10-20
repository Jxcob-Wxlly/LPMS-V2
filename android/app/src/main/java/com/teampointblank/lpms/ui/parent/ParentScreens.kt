package com.teampointblank.lpms.ui.parent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.LearnerAttendance
import com.teampointblank.lpms.data.LearnerSummary
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.domain.ReportLogic
import com.teampointblank.lpms.ui.components.AccentChip
import com.teampointblank.lpms.ui.components.Avatar
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.HeaderBlock
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.LpmsSnackbarHost
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.PrimaryButton
import com.teampointblank.lpms.ui.components.SectionHeader
import com.teampointblank.lpms.ui.components.StatTile
import com.teampointblank.lpms.ui.components.StatusChip
import com.teampointblank.lpms.ui.components.pullUp
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.report.exportAndShareReport
import com.teampointblank.lpms.ui.theme.LpmsColors
import kotlinx.coroutines.launch

/** Parent home: the signed-in parent's children, each with attendance compared with their expectation. */
@Composable
fun ParentHomeScreen(
    viewModel: ParentHomeViewModel,
    onNavigate: (String) -> Unit,
    onOpenChild: (Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val palette = LpmsColors.palette

    MainScaffold(currentRoute = Routes.PARENT_HOME, onNavigate = onNavigate, isParent = true) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HeaderBlock(bottomPadding = 28.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "LPMS",
                        modifier = Modifier.weight(1f),
                        color = palette.onHeaderMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Refresh", tint = palette.onHeader)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("My children", color = palette.onHeader, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your attendance expectation: $target%",
                    color = palette.onHeaderMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isLoading && state.children.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
                }
                state.error?.let { if (state.children.isNotEmpty()) InfoBanner(text = it, isError = true) }
                when {
                    state.children.isEmpty() && state.isLoading -> Box(Modifier.fillMaxWidth().height(200.dp)) { LoadingBox() }
                    state.children.isEmpty() && state.error != null -> Box(Modifier.fillMaxWidth().height(320.dp)) {
                        ErrorState(message = state.error ?: "Something went wrong.", onRetry = { viewModel.load() })
                    }
                    state.children.isEmpty() -> DashedEmpty(
                        "Your account is not linked to a learner yet. Please contact READ Educational Trust."
                    )
                    else -> state.children.forEach { child ->
                        ChildCard(child = child, target = target, onClick = { onOpenChild(child.learnerId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetChip(rate: Double?, target: Int) {
    val colors = LpmsColors.status
    when (ReportLogic.targetStatus(rate, target)) {
        ReportLogic.TargetStatus.Met -> StatusChip("Meets $target%", colors.successContainer, colors.onSuccessContainer)
        ReportLogic.TargetStatus.Below -> StatusChip("Below $target%", colors.warningContainer, colors.onWarningContainer)
        ReportLogic.TargetStatus.NoData -> StatusChip("No attendance yet", colors.infoContainer, colors.onInfoContainer)
    }
}

@Composable
private fun ChildCard(child: LearnerSummary, target: Int, onClick: () -> Unit) {
    val palette = LpmsColors.palette
    LpmsCard(onClick = onClick, shape = RoundedCornerShape(32.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(Formatters.initials(child.firstName, child.lastName), size = 58.dp)
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Formatters.fullName(child.firstName, child.lastName),
                        color = palette.text,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp)
                    )
                    Text(
                        "${child.grade} · ${child.schoolName}",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("${Formatters.score(child.averageScore)} avg", color = palette.text, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${Formatters.percent(child.attendanceRate)} attendance",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            TargetChip(child.attendanceRate, target)
            Text(
                "Tap to see the full report and download the PDF.",
                color = palette.textMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** One child's report: scores, attendance against the parent's expectation, history and the PDF button. */
@Composable
fun ChildReportScreen(viewModel: ChildReportViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LpmsColors.palette

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    val progress = state.progress
    val download: () -> Unit = {
        if (progress != null && !state.isExporting) {
            viewModel.setExporting(true)
            scope.launch {
                val error = exportAndShareReport(context, progress, target)
                viewModel.setExporting(false)
                if (error != null) viewModel.showMessage(error)
            }
        }
    }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { LpmsSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                progress == null && state.isLoading -> LoadingBox()
                progress == null -> ErrorState(
                    message = state.error ?: "Something went wrong. Please try again.",
                    onRetry = { viewModel.load() }
                )
                else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val learner = progress.learner
                    HeaderBlock(bottomPadding = 64.dp) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.load() }) {
                                Icon(Icons.Outlined.Sync, contentDescription = "Refresh", tint = palette.onHeader)
                            }
                            IconButton(onClick = download, enabled = !state.isExporting) {
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
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp)
                        )
                        Spacer(Modifier.height(14.dp))
                        AccentChip(progress.progressLabel.ifBlank { "No assessments yet" })
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(modifier = Modifier.pullUp(44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("Average score", Formatters.score(learner.averageScore), Modifier.weight(1f))
                            StatTile("Attendance", Formatters.percent(learner.attendanceRate), Modifier.weight(1f))
                        }
                        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
                        state.error?.let { InfoBanner(text = it, isError = true) }

                        TargetChip(learner.attendanceRate, target)
                        InfoBanner(text = ReportLogic.targetMessage(learner.attendanceRate, target))
                        PrimaryButton(
                            text = "Download PDF report",
                            onClick = download,
                            enabled = !state.isExporting,
                            loading = state.isExporting
                        )

                        SectionHeader("Assessments")
                        if (progress.assessments.isEmpty()) {
                            DashedEmpty("Your child's teacher has not recorded an assessment yet.")
                        } else {
                            progress.assessments.forEach { a ->
                                LpmsCard(shape = RoundedCornerShape(32.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            Formatters.score(a.assessmentScore),
                                            modifier = Modifier.padding(end = 26.dp),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = palette.accent
                                        )
                                        Column {
                                            Text(
                                                Formatters.weekdayDate(a.assessmentDate),
                                                color = palette.text,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            val notes = a.notes
                                            if (!notes.isNullOrBlank()) {
                                                Text(notes, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                "Recorded by ${a.recordedBy}",
                                                color = palette.textMuted,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
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
                            progress.attendance.take(15).forEach { AttendanceLine(it) }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceLine(entry: LearnerAttendance) {
    val colors = LpmsColors.status
    val palette = LpmsColors.palette
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Formatters.date(entry.sessionDate), color = palette.text, style = MaterialTheme.typography.bodyLarge)
            Text(entry.programmeName, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (entry.status == "Present") {
            StatusChip("Present", colors.successContainer, colors.onSuccessContainer)
        } else {
            StatusChip(entry.status.ifBlank { "-" }, colors.warningContainer, colors.onWarningContainer)
        }
    }
}
