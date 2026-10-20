package com.teampointblank.lpms.domain

import java.net.URI
import java.time.LocalDate
import java.time.LocalTime

/** Result of validating/parsing one input: either a [value] or an [error] message. */
data class FieldResult<T>(val value: T? = null, val error: String? = null)

/**
 * Input validation for every form in the app. Pure Kotlin (no Android classes) so it is covered by
 * fast unit tests. Messages are written for teachers, not developers.
 */
object Validators {
    const val MAX_LOGIN_LENGTH = 100
    const val MAX_NOTES_LENGTH = 255
    const val MAX_SEARCH_LENGTH = 50
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 100

    private val scorePattern = Regex("^\\d+(\\.\\d{1,2})?$")

    fun validateIdentifier(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Enter your email address or username."
            v.length > MAX_LOGIN_LENGTH -> "That is too long (maximum $MAX_LOGIN_LENGTH characters)."
            v.any { it.isWhitespace() } -> "Your email address or username can't contain spaces."
            else -> null
        }
    }

    fun validateLoginPassword(value: String): String? = when {
        value.isEmpty() -> "Enter your password."
        value.length > MAX_PASSWORD_LENGTH -> "That is too long (maximum $MAX_PASSWORD_LENGTH characters)."
        else -> null
    }

    private fun passwordRuleError(value: String): String? = when {
        value.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters."
        value.length > MAX_PASSWORD_LENGTH -> "Use $MAX_PASSWORD_LENGTH characters or fewer."
        value.none { it.isUpperCase() } -> "Include at least one uppercase letter."
        value.none { it.isLowerCase() } -> "Include at least one lowercase letter."
        value.none { it.isDigit() } -> "Include at least one number."
        else -> null
    }

    /** Same rules as the API's PasswordPolicy, plus "different from the current password". */
    fun validateNewPassword(value: String, current: String): String? =
        passwordRuleError(value) ?: if (value == current) "Choose a password that is different from your current one." else null

    /** Password rules for a brand-new account (same as the API's PasswordPolicy). */
    fun validateRegisterPassword(value: String): String? = passwordRuleError(value)

    const val MAX_NAME_LENGTH = 100
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** A teacher must give a first and a last name. */
    fun validateFullName(value: String): String? {
        val parts = value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "Enter your name."
            parts.size < 2 -> "Enter your first and last name."
            value.trim().length > MAX_NAME_LENGTH -> "That is too long (maximum $MAX_NAME_LENGTH characters)."
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Enter your email address."
            v.length > MAX_LOGIN_LENGTH -> "That is too long (maximum $MAX_LOGIN_LENGTH characters)."
            !emailPattern.matches(v) -> "Enter a valid email address, such as name@school.org."
            else -> null
        }
    }

    fun validateSchoolChoice(schoolId: Int?): String? = if (schoolId == null) "Choose your school." else null

    fun validateProgrammeChoice(programmeId: Int?): String? = if (programmeId == null) "Choose a programme." else null

    /** A new session must be today or later and inside the programme's start and end dates. */
    fun validateSessionDate(date: LocalDate?, today: LocalDate, programmeStart: LocalDate?, programmeEnd: LocalDate?): String? = when {
        date == null -> "Choose the session date."
        date.isBefore(today) -> "The session date can't be in the past."
        programmeStart != null && date.isBefore(programmeStart) -> "The programme only starts on ${programmeStart}."
        programmeEnd != null && date.isAfter(programmeEnd) -> "The programme ends on ${programmeEnd}."
        else -> null
    }

    fun validateSessionTimes(start: LocalTime?, end: LocalTime?): String? = when {
        start == null -> "Choose the start time."
        end == null -> "Choose the end time."
        !end.isAfter(start) -> "The end time must be after the start time."
        else -> null
    }

    fun validateCurrentPassword(value: String): String? =
        if (value.isEmpty()) "Enter your current password." else null

    fun validateConfirmPassword(newPassword: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Re-enter the new password."
        confirm != newPassword -> "The passwords don't match."
        else -> null
    }

    /** Accepts "72", "72.5" and "72,5" (comma decimals are common in South Africa). */
    fun parseScore(text: String): FieldResult<Double> {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) return FieldResult(error = "Enter a score between 0 and 100.")
        if (!scorePattern.matches(cleaned)) {
            return FieldResult(error = "Enter a number such as 72 or 72.5 (up to 2 decimal places).")
        }
        val value = cleaned.toDoubleOrNull()
            ?: return FieldResult(error = "Enter a number such as 72 or 72.5.")
        if (value > 100.0) return FieldResult(error = "The score must be between 0 and 100.")
        return FieldResult(value = value)
    }

    fun validateAssessmentDate(date: LocalDate?, today: LocalDate): String? = when {
        date == null -> "Choose the assessment date."
        date.isAfter(today) -> "The assessment date can't be in the future."
        else -> null
    }

    fun validateNotes(notes: String): String? =
        if (notes.length > MAX_NOTES_LENGTH) "Notes can be at most $MAX_NOTES_LENGTH characters." else null

    /** Keeps only characters that can be part of a score while the user types. */
    fun filterScoreInput(text: String): String =
        text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)

    /**
     * Checks a server address typed in Settings and returns it in the form Retrofit needs
     * (scheme://host[:port]/path/ with a trailing slash). A missing scheme is treated as https.
     */
    fun normalizeServerUrl(input: String): FieldResult<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return FieldResult(error = "Enter the server address.")
        if (trimmed.any { it.isWhitespace() }) return FieldResult(error = "The address can't contain spaces.")

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = try {
            URI(withScheme)
        } catch (e: Exception) {
            return FieldResult(error = "That doesn't look like a valid web address.")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return FieldResult(error = "The address must start with http:// or https://")
        }
        val host = uri.host
        if (host.isNullOrBlank()) return FieldResult(error = "That doesn't look like a valid web address.")
        if (uri.rawUserInfo != null) return FieldResult(error = "Remove the user name from the address.")
        if (uri.rawQuery != null || uri.rawFragment != null) {
            return FieldResult(error = "Remove anything after a ? or # from the address.")
        }

        val port = if (uri.port > 0) ":${uri.port}" else ""
        var path = uri.rawPath ?: ""
        if (!path.endsWith("/")) path += "/"
        return FieldResult(value = "$scheme://${host.lowercase()}$port$path")
    }
}

/** Helpers for the sign-in token's expiry time. */
object TokenRules {
    /** True when [expiresAtUtc] (ISO-8601, e.g. 2026-10-16T12:00:00Z) is in the past. Unreadable values count as not expired. */
    fun isExpired(expiresAtUtc: String, now: java.time.Instant): Boolean {
        return try {
            java.time.Instant.parse(expiresAtUtc).isBefore(now)
        } catch (e: Exception) {
            false
        }
    }
}
