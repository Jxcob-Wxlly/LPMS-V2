package com.teampointblank.lpms.domain

import com.teampointblank.lpms.data.RosterEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttendanceLogicTest {

    private val roster = listOf(
        RosterEntry(1, "Amahle", "Dlamini", "Grade 3", null),
        RosterEntry(2, "Bongani", "Khumalo", "Grade 3", "Present"),
        RosterEntry(3, "Citrine", "Naidoo", "Grade 4", "Absent")
    )

    @Test fun initialDraft_containsOnlyRecordedLearners() {
        assertEquals(mapOf(2 to "Present", 3 to "Absent"), AttendanceLogic.initialDraft(roster))
    }

    @Test fun summary_countsEachState() {
        val s = AttendanceLogic.summary(roster, AttendanceLogic.initialDraft(roster))
        assertEquals(1, s.present)
        assertEquals(1, s.absent)
        assertEquals(1, s.notMarked)
    }

    @Test fun setStatus_ignoresInvalidStatus() {
        val draft = AttendanceLogic.setStatus(emptyMap(), 1, "Maybe")
        assertTrue(draft.isEmpty())
        assertEquals(mapOf(1 to "Present"), AttendanceLogic.setStatus(emptyMap(), 1, "Present"))
    }

    @Test fun markAll_marksEveryLearner() {
        val draft = AttendanceLogic.markAll(roster, AttendanceLogic.PRESENT)
        assertEquals(3, draft.size)
        assertTrue(draft.values.all { it == "Present" })
    }

    @Test fun hasChanges_detectsEdits() {
        val saved = AttendanceLogic.initialDraft(roster)
        assertFalse(AttendanceLogic.hasChanges(roster, saved))
        assertTrue(AttendanceLogic.hasChanges(roster, saved + (1 to "Present")))
        assertTrue(AttendanceLogic.hasChanges(roster, saved + (2 to "Absent")))
    }

    @Test fun validateForSave_requiresEveryLearnerMarked() {
        assertNotNull(AttendanceLogic.validateForSave(roster, AttendanceLogic.initialDraft(roster)))
        assertNull(AttendanceLogic.validateForSave(roster, AttendanceLogic.markAll(roster, "Absent")))
        assertNotNull(AttendanceLogic.validateForSave(emptyList(), emptyMap()))
    }

    @Test fun entriesToSave_followRosterOrder() {
        val draft = mapOf(3 to "Absent", 1 to "Present", 2 to "Present")
        val entries = AttendanceLogic.entriesToSave(roster, draft)
        assertEquals(listOf(1, 2, 3), entries.map { it.learnerId })
        assertEquals(listOf("Present", "Present", "Absent"), entries.map { it.status })
    }

    @Test fun canRecord_onlyFromTheDayOfTheSession() {
        val today = LocalDate.of(2026, 10, 16)
        assertTrue(AttendanceLogic.canRecord("2026-10-16", today))
        assertTrue(AttendanceLogic.canRecord("2026-10-01", today))
        assertFalse(AttendanceLogic.canRecord("2026-10-17", today))
        assertTrue(AttendanceLogic.canRecord("garbage", today))
    }
}
