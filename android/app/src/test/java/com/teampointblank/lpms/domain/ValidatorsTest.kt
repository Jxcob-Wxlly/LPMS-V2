package com.teampointblank.lpms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {

    // ----- sign-in -----

    @Test fun identifier_blank_isRejected() {
        assertEquals("Enter your email address or username.", Validators.validateIdentifier(""))
        assertEquals("Enter your email address or username.", Validators.validateIdentifier("   "))
    }

    @Test fun identifier_withSpaces_isRejected() {
        assertNotNull(Validators.validateIdentifier("teacher 01"))
    }

    @Test fun identifier_tooLong_isRejected() {
        assertNotNull(Validators.validateIdentifier("a".repeat(101)))
    }

    @Test fun identifier_usernameOrEmail_isAccepted() {
        assertNull(Validators.validateIdentifier("teacher01"))
        assertNull(Validators.validateIdentifier("  thandi@read.org.za  "))
    }

    @Test fun loginPassword_blank_isRejected() {
        assertEquals("Enter your password.", Validators.validateLoginPassword(""))
        assertNull(Validators.validateLoginPassword("x"))
    }

    // ----- new password -----

    @Test fun newPassword_tooShort() {
        assertEquals("Use at least 8 characters.", Validators.validateNewPassword("Ab1", "old"))
    }

    @Test fun newPassword_needsUpperLowerAndDigit() {
        assertEquals("Include at least one uppercase letter.", Validators.validateNewPassword("lowercase1", "old"))
        assertEquals("Include at least one lowercase letter.", Validators.validateNewPassword("UPPERCASE1", "old"))
        assertEquals("Include at least one number.", Validators.validateNewPassword("NoDigitsHere", "old"))
    }

    @Test fun newPassword_mustDifferFromCurrent() {
        assertNotNull(Validators.validateNewPassword("Passw0rdOK", "Passw0rdOK"))
    }

    @Test fun newPassword_valid() {
        assertNull(Validators.validateNewPassword("Fresh2026", "Lpms@2026!"))
    }

    @Test fun confirmPassword_mustMatch() {
        assertEquals("Re-enter the new password.", Validators.validateConfirmPassword("Abcdef12", ""))
        assertEquals("The passwords don't match.", Validators.validateConfirmPassword("Abcdef12", "Abcdef13"))
        assertNull(Validators.validateConfirmPassword("Abcdef12", "Abcdef12"))
    }

    // ----- scores -----

    @Test fun score_wholeAndDecimal() {
        assertEquals(72.0, Validators.parseScore("72").value!!, 0.0001)
        assertEquals(72.5, Validators.parseScore("72.5").value!!, 0.0001)
        assertEquals(0.0, Validators.parseScore("0").value!!, 0.0001)
        assertEquals(100.0, Validators.parseScore("100").value!!, 0.0001)
    }

    @Test fun score_acceptsCommaDecimal() {
        assertEquals(72.5, Validators.parseScore(" 72,5 ").value!!, 0.0001)
    }

    @Test fun score_outOfRange_isRejected() {
        assertEquals("The score must be between 0 and 100.", Validators.parseScore("101").error)
        assertEquals("The score must be between 0 and 100.", Validators.parseScore("100.01").error)
    }

    @Test fun score_notANumber_isRejected() {
        assertNull(Validators.parseScore("abc").value)
        assertNotNull(Validators.parseScore("abc").error)
        assertNotNull(Validators.parseScore("-5").error)
        assertNotNull(Validators.parseScore("7.555").error)
        assertNotNull(Validators.parseScore("1e5").error)
        assertEquals("Enter a score between 0 and 100.", Validators.parseScore("").error)
    }

    @Test fun scoreInput_filtersUnwantedCharacters() {
        assertEquals("72.5", Validators.filterScoreInput("7a2.5x"))
        assertEquals("123456", Validators.filterScoreInput("1234567"))
    }

    // ----- dates and notes -----

    @Test fun assessmentDate_rules() {
        val today = LocalDate.of(2026, 10, 16)
        assertEquals("Choose the assessment date.", Validators.validateAssessmentDate(null, today))
        assertEquals("The assessment date can't be in the future.", Validators.validateAssessmentDate(today.plusDays(1), today))
        assertNull(Validators.validateAssessmentDate(today, today))
        assertNull(Validators.validateAssessmentDate(today.minusDays(30), today))
    }

    @Test fun notes_limit() {
        assertNull(Validators.validateNotes("a".repeat(255)))
        assertNotNull(Validators.validateNotes("a".repeat(256)))
    }

    // ----- server address -----

    @Test fun serverUrl_addsHttpsAndTrailingSlash() {
        assertEquals("https://lpms-api.azurewebsites.net/", Validators.normalizeServerUrl("lpms-api.azurewebsites.net").value)
        assertEquals("https://lpms-api.azurewebsites.net/", Validators.normalizeServerUrl("  https://LPMS-API.azurewebsites.net  ").value)
    }

    @Test fun serverUrl_keepsPortAndPath() {
        assertEquals("http://192.168.1.5:5080/", Validators.normalizeServerUrl("http://192.168.1.5:5080").value)
        assertEquals("https://example.com/lpms/", Validators.normalizeServerUrl("https://example.com/lpms").value)
    }

    @Test fun serverUrl_rejectsBadInput() {
        assertNotNull(Validators.normalizeServerUrl("").error)
        assertNotNull(Validators.normalizeServerUrl("has space.com").error)
        assertNotNull(Validators.normalizeServerUrl("ftp://example.com").error)
        assertNotNull(Validators.normalizeServerUrl("https://").error)
        assertNotNull(Validators.normalizeServerUrl("https://example.com/?a=1").error)
        assertTrue(Validators.normalizeServerUrl("ftp://example.com").value == null)
    }

    // ----- create account -----

    @Test fun fullName_needsFirstAndLast() {
        assertEquals("Enter your name.", Validators.validateFullName("  "))
        assertEquals("Enter your first and last name.", Validators.validateFullName("Sipho"))
        assertNull(Validators.validateFullName("Sipho Mokoena"))
        assertNull(Validators.validateFullName("  Thandi   van der Merwe "))
    }

    @Test fun email_mustLookLikeAnEmail() {
        assertEquals("Enter your email address.", Validators.validateEmail(""))
        assertNotNull(Validators.validateEmail("sipho"))
        assertNotNull(Validators.validateEmail("sipho@school"))
        assertNotNull(Validators.validateEmail("sipho @school.org"))
        assertNull(Validators.validateEmail(" sipho@school.org "))
        assertNotNull(Validators.validateEmail("a".repeat(100) + "@x.org"))
    }

    @Test fun registerPassword_followsTheSameRulesAsTheApi() {
        assertNotNull(Validators.validateRegisterPassword("short1A"))
        assertNotNull(Validators.validateRegisterPassword("alllowercase1"))
        assertNotNull(Validators.validateRegisterPassword("ALLUPPERCASE1"))
        assertNotNull(Validators.validateRegisterPassword("NoDigitsHere"))
        assertNull(Validators.validateRegisterPassword("Passw0rd!"))
    }

    @Test fun schoolAndProgrammeChoice_mustBeMade() {
        assertEquals("Choose your school.", Validators.validateSchoolChoice(null))
        assertNull(Validators.validateSchoolChoice(3))
        assertEquals("Choose a programme.", Validators.validateProgrammeChoice(null))
        assertNull(Validators.validateProgrammeChoice(1))
    }

    // ----- new session -----

    private val today = LocalDate.of(2026, 10, 19)
    private val progStart = LocalDate.of(2026, 1, 1)
    private val progEnd = LocalDate.of(2026, 12, 15)

    @Test fun sessionDate_mustBeChosen() {
        assertEquals("Choose the session date.", Validators.validateSessionDate(null, today, progStart, progEnd))
    }

    @Test fun sessionDate_cannotBeInThePast() {
        assertNotNull(Validators.validateSessionDate(today.minusDays(1), today, progStart, progEnd))
        assertNull(Validators.validateSessionDate(today, today, progStart, progEnd))
    }

    @Test fun sessionDate_mustBeInsideTheProgramme() {
        assertNotNull(Validators.validateSessionDate(LocalDate.of(2027, 1, 5), today, progStart, progEnd))
        assertNull(Validators.validateSessionDate(LocalDate.of(2026, 12, 15), today, progStart, progEnd))
    }

    @Test fun sessionTimes_endMustBeAfterStart() {
        assertEquals("Choose the start time.", Validators.validateSessionTimes(null, java.time.LocalTime.of(10, 0)))
        assertEquals("Choose the end time.", Validators.validateSessionTimes(java.time.LocalTime.of(9, 0), null))
        assertNotNull(Validators.validateSessionTimes(java.time.LocalTime.of(10, 0), java.time.LocalTime.of(10, 0)))
        assertNotNull(Validators.validateSessionTimes(java.time.LocalTime.of(10, 0), java.time.LocalTime.of(9, 0)))
        assertNull(Validators.validateSessionTimes(java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0)))
    }

    // ----- messages -----

    @Test fun message_blank_isRejected() {
        assertEquals("Type a message first.", Validators.validateMessage(""))
        assertEquals("Type a message first.", Validators.validateMessage("   "))
    }

    @Test fun message_atTheLimit_isAccepted() {
        assertNull(Validators.validateMessage("a".repeat(Validators.MAX_MESSAGE_LENGTH)))
    }

    @Test fun message_overTheLimit_isRejected() {
        assertNotNull(Validators.validateMessage("a".repeat(Validators.MAX_MESSAGE_LENGTH + 1)))
    }

    @Test fun message_normalText_isAccepted() {
        assertNull(Validators.validateMessage("Good morning, Ms Zulu."))
    }
}
