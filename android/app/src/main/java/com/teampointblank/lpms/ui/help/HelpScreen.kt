package com.teampointblank.lpms.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.LpmsTopBar
import com.teampointblank.lpms.ui.theme.LpmsColors

private data class HelpTopic(val question: String, val answer: String)

private val topics = listOf(
    HelpTopic(
        "I'm a parent - what can I see?",
        "After you sign in with the username your school gave you, My children shows each of your children with their " +
            "average score and attendance. Tap a child for the full report: every assessment, recent attendance and a " +
            "Download PDF report button. You can only ever see your own children."
    ),
    HelpTopic(
        "How do I get my child's report as a PDF?",
        "Open My children, tap your child, then tap Download PDF report (or the share icon at the top). Your phone's " +
            "share sheet opens, where you can save the PDF to Files or Drive, print it, or send it by WhatsApp or email."
    ),
    HelpTopic(
        "What is the attendance expectation?",
        "It is the attendance percentage you expect from your child. Set it under Settings > Attendance expectation " +
            "(parents only). The app then shows whether your child meets it or is below it, on the children list, " +
            "the report screen and in the PDF."
    ),
    HelpTopic(
        "How do I sign in?",
        "Type the email address or username your coordinator gave you, then your password, and tap Sign in. " +
            "Tap Show to check what you typed. If you've forgotten your password, ask your administrator to reset it."
    ),
    HelpTopic(
        "The app says it can't reach the server",
        "Check that your phone has mobile data or Wi-Fi. If it does, tap Server settings on the sign-in screen " +
            "(or Settings > Change server address) and make sure the address is correct, then tap Save and test. " +
            "The server can take up to a minute to wake up the first time you use it in a while - wait and try again."
    ),
    HelpTopic(
        "How do I take attendance?",
        "Open the Sessions tab (or tap a session on the Home screen). Choose the session, then mark each learner " +
            "Present or Absent. \"All present\" marks everyone in one tap so you only change the absentees. " +
            "When every learner is marked, tap Save attendance. You can come back later and correct it."
    ),
    HelpTopic(
        "Why can't I take attendance for a session?",
        "Attendance opens on the day of the session. Sessions that are still in the future are read-only. " +
            "You can only take attendance for your own sessions."
    ),
    HelpTopic(
        "How do I record a learner's progress?",
        "Open the Learners tab, tap a learner, then tap Add assessment. Enter a score from 0 to 100, choose the " +
            "date of the assessment (not in the future) and add a note if you like. You can edit or delete the " +
            "assessments you recorded using the pencil and bin icons."
    ),
    HelpTopic(
        "How do I find a learner?",
        "On the Learners tab, type part of the learner's first or last name in the search box. " +
            "Tap the cross to clear the search."
    ),
    HelpTopic(
        "How do I change my password?",
        "Go to Settings > Change password. Enter your current password and a new one with at least 8 characters, " +
            "including an uppercase letter, a lowercase letter and a number."
    ),
    HelpTopic(
        "How do I change the theme or text size?",
        "In Settings > Appearance, choose Light, Dark or Use phone setting, and switch on Larger text if the " +
            "letters are hard to read."
    ),
    HelpTopic(
        "Is my information safe?",
        "Your password is never saved on the phone. The app keeps a sign-in token in encrypted storage and " +
            "removes it when you sign out. Passwords are stored on the server as one-way hashes."
    ),
    HelpTopic(
        "Who do I contact for more help?",
        "Speak to your READ Educational Trust programme coordinator or administrator."
    )
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val palette = LpmsColors.palette
    Scaffold(
        containerColor = palette.background,
        topBar = { LpmsTopBar(title = "Help & FAQ", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tap a question to see the answer.",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textMuted
            )
            topics.forEachIndexed { index, topic ->
                HelpItem(index = index, topic = topic)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HelpItem(index: Int, topic: HelpTopic) {
    val palette = LpmsColors.palette
    var expanded by rememberSaveable(index) { mutableStateOf(false) }
    LpmsCard(onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    topic.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(8.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = palette.text
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(topic.answer, style = MaterialTheme.typography.bodyLarge, color = palette.textMuted)
            }
        }
    }
}
