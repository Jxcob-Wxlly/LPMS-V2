package com.teampointblank.lpms.domain

/** Pure rules for the parent view and the PDF report (unit tested, no Android classes). */
object ReportLogic {
    const val MIN_TARGET = 50
    const val MAX_TARGET = 100
    const val DEFAULT_TARGET = 80

    enum class TargetStatus { Met, Below, NoData }

    /** Keeps a saved or typed expectation inside the allowed range. */
    fun clampTarget(value: Int): Int = value.coerceIn(MIN_TARGET, MAX_TARGET)

    fun targetStatus(attendanceRate: Double?, target: Int): TargetStatus = when {
        attendanceRate == null -> TargetStatus.NoData
        attendanceRate >= target -> TargetStatus.Met
        else -> TargetStatus.Below
    }

    /** One plain-language sentence comparing the child's attendance with the parent's expectation. */
    fun targetMessage(attendanceRate: Double?, target: Int): String =
        when (targetStatus(attendanceRate, target)) {
            TargetStatus.NoData -> "No attendance has been recorded yet, so there is nothing to compare with your $target% expectation."
            TargetStatus.Met -> "Attendance is ${Formatters.percent(attendanceRate)} - this meets your expectation of $target%."
            TargetStatus.Below -> "Attendance is ${Formatters.percent(attendanceRate)} - below your expectation of $target%."
        }

    /** Whole sessions the child would still need to attend in a row to reach the target (0 when already met). */
    fun sessionsToReachTarget(present: Int, total: Int, target: Int): Int {
        if (total <= 0 || present < 0 || present > total) return 0
        val t = clampTarget(target)
        if (t >= 100) return if (present == total) 0 else -1   // 100% can never be regained
        var extra = 0
        while ((present + extra) * 100.0 / (total + extra) < t && extra < 1000) extra++
        return extra
    }

    /** Safe file name such as LPMS-report-Amahle-Dlamini-2026-10-16.pdf. */
    fun fileName(firstName: String, lastName: String, dateIso: String): String {
        val name = "$firstName $lastName".trim()
            .replace(Regex("[^A-Za-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "learner" }
        val date = dateIso.replace(Regex("[^0-9-]"), "").ifEmpty { "report" }
        return "LPMS-report-$name-$date.pdf"
    }
}
