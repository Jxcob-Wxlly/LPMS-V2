package com.teampointblank.lpms.domain

import com.teampointblank.lpms.data.AttendanceEntry
import com.teampointblank.lpms.data.RosterEntry
import java.time.LocalDate

data class AttendanceSummary(val present: Int, val absent: Int, val notMarked: Int)

/** Rules for the "take attendance" screen. The draft maps learnerId -> "Present" | "Absent". */
object AttendanceLogic {
    const val PRESENT = "Present"
    const val ABSENT = "Absent"

    private fun isValid(status: String?): Boolean = status == PRESENT || status == ABSENT

    /** The attendance already saved on the server. */
    fun initialDraft(roster: List<RosterEntry>): Map<Int, String> =
        roster.filter { isValid(it.status) }.associate { it.learnerId to it.status!! }

    fun setStatus(draft: Map<Int, String>, learnerId: Int, status: String): Map<Int, String> =
        if (isValid(status)) draft + (learnerId to status) else draft

    fun markAll(roster: List<RosterEntry>, status: String): Map<Int, String> =
        if (isValid(status)) roster.associate { it.learnerId to status } else emptyMap()

    fun summary(roster: List<RosterEntry>, draft: Map<Int, String>): AttendanceSummary {
        val present = roster.count { draft[it.learnerId] == PRESENT }
        val absent = roster.count { draft[it.learnerId] == ABSENT }
        return AttendanceSummary(present, absent, roster.size - present - absent)
    }

    fun hasChanges(roster: List<RosterEntry>, draft: Map<Int, String>): Boolean {
        val saved = initialDraft(roster)
        return roster.any { saved[it.learnerId] != draft[it.learnerId] }
    }

    /** Entries in roster order, ready to send to the API. Learners that are not marked are left out. */
    fun entriesToSave(roster: List<RosterEntry>, draft: Map<Int, String>): List<AttendanceEntry> =
        roster.mapNotNull { r -> draft[r.learnerId]?.let { AttendanceEntry(r.learnerId, it) } }

    /** A message describing why the draft can't be saved yet, or null when it can. */
    fun validateForSave(roster: List<RosterEntry>, draft: Map<Int, String>): String? {
        if (roster.isEmpty()) return "There are no learners to mark for this session."
        val notMarked = summary(roster, draft).notMarked
        if (notMarked > 0) {
            return "Mark every learner as Present or Absent first ($notMarked still to mark)."
        }
        return null
    }

    /** Attendance can be taken on the day of the session or afterwards. Unreadable dates are left to the server. */
    fun canRecord(sessionDateIso: String, today: LocalDate): Boolean {
        val date = Formatters.parseDate(sessionDateIso) ?: return true
        return !date.isAfter(today)
    }
}
