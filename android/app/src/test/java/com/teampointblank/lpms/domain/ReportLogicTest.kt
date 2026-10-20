package com.teampointblank.lpms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLogicTest {

    @Test fun clampTarget_keeps_values_inside_the_range() {
        assertEquals(50, ReportLogic.clampTarget(10))
        assertEquals(100, ReportLogic.clampTarget(250))
        assertEquals(85, ReportLogic.clampTarget(85))
    }

    @Test fun status_is_met_when_rate_equals_target() {
        assertEquals(ReportLogic.TargetStatus.Met, ReportLogic.targetStatus(80.0, 80))
    }

    @Test fun status_is_met_when_rate_is_above_target() {
        assertEquals(ReportLogic.TargetStatus.Met, ReportLogic.targetStatus(95.5, 80))
    }

    @Test fun status_is_below_when_rate_is_under_target() {
        assertEquals(ReportLogic.TargetStatus.Below, ReportLogic.targetStatus(79.9, 80))
    }

    @Test fun status_is_nodata_without_a_rate() {
        assertEquals(ReportLogic.TargetStatus.NoData, ReportLogic.targetStatus(null, 80))
    }

    @Test fun message_mentions_rate_and_target_when_met() {
        val text = ReportLogic.targetMessage(90.0, 80)
        assertTrue(text.contains("90%"))
        assertTrue(text.contains("meets"))
        assertTrue(text.contains("80%"))
    }

    @Test fun message_says_below_when_under_target() {
        val text = ReportLogic.targetMessage(60.0, 75)
        assertTrue(text.contains("below"))
        assertTrue(text.contains("75%"))
    }

    @Test fun message_handles_missing_attendance() {
        assertTrue(ReportLogic.targetMessage(null, 80).contains("No attendance"))
    }

    @Test fun sessionsToReach_is_zero_when_already_met() {
        assertEquals(0, ReportLogic.sessionsToReachTarget(present = 8, total = 10, target = 80))
    }

    @Test fun sessionsToReach_counts_extra_sessions() {
        // 6 of 10 is 60%; 16 of 20 is the first point that reaches 80%.
        assertEquals(10, ReportLogic.sessionsToReachTarget(present = 6, total = 10, target = 80))
    }

    @Test fun sessionsToReach_is_zero_without_sessions() {
        assertEquals(0, ReportLogic.sessionsToReachTarget(present = 0, total = 0, target = 80))
    }

    @Test fun sessionsToReach_for_full_target_is_impossible_after_an_absence() {
        assertEquals(-1, ReportLogic.sessionsToReachTarget(present = 9, total = 10, target = 100))
    }

    @Test fun fileName_is_safe_and_dated() {
        assertEquals(
            "LPMS-report-Amahle-Dlamini-2026-10-16.pdf",
            ReportLogic.fileName("Amahle", "Dlamini", "2026-10-16")
        )
    }

    @Test fun fileName_removes_unsafe_characters() {
        assertEquals(
            "LPMS-report-Zoe-O-Neil-2026-10-16.pdf",
            ReportLogic.fileName("Zoe", "O'Neil", "2026-10-16")
        )
    }

    @Test fun fileName_falls_back_when_names_are_blank() {
        assertEquals("LPMS-report-learner-2026-10-16.pdf", ReportLogic.fileName("", " ", "2026-10-16"))
    }
}
