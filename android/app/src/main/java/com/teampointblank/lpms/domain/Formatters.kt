package com.teampointblank.lpms.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display formatting. Every function falls back to the raw text instead of throwing on bad data. */
object Formatters {
    private val longDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val weekdayDateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val southAfrica: ZoneId = ZoneId.of("Africa/Johannesburg")

    fun today(): LocalDate = LocalDate.now(southAfrica)

    fun parseDate(iso: String?): LocalDate? = try {
        if (iso.isNullOrBlank()) null else LocalDate.parse(iso.take(10))
    } catch (e: Exception) {
        null
    }

    fun date(iso: String?): String = parseDate(iso)?.format(longDateFormat) ?: (iso ?: "")

    /** "Mon 19 Oct" (falls back to the raw text when it is not a date). */
    fun weekdayDate(iso: String?): String = parseDate(iso)?.format(weekdayDateFormat) ?: (iso ?: "")

    fun shortDate(iso: String?): String = parseDate(iso)?.format(shortDateFormat) ?: (iso ?: "")

    fun shortDate(date: LocalDate?): String = date?.format(shortDateFormat) ?: ""

    fun time(iso: String?): String = try {
        if (iso.isNullOrBlank()) "" else LocalTime.parse(iso).format(clockFormat)
    } catch (e: Exception) {
        iso ?: ""
    }

    fun timeRange(start: String?, end: String?): String = "${time(start)} - ${time(end)}"

    /** "66.7%" or "85%"; "-" when there is no value yet. */
    fun percent(value: Double?): String = when {
        value == null -> "-"
        value == Math.rint(value) -> String.format(Locale.ENGLISH, "%.0f%%", value)
        else -> String.format(Locale.ENGLISH, "%.1f%%", value)
    }

    /** "72.5" or "80"; "-" when there is no value yet. */
    fun score(value: Double?): String = when {
        value == null -> "-"
        value == Math.rint(value) -> String.format(Locale.ENGLISH, "%.0f", value)
        else -> String.format(Locale.ENGLISH, "%.1f", value)
    }

    fun fullName(first: String, last: String): String = "$first $last".trim()

    fun initials(first: String, last: String): String {
        val a = first.trim().firstOrNull()?.uppercaseChar()
        val b = last.trim().firstOrNull()?.uppercaseChar()
        return listOfNotNull(a, b).joinToString("").ifEmpty { "?" }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    fun plural(count: Int, singular: String, plural: String = singular + "s"): String =
        "$count ${if (count == 1) singular else plural}"
}
