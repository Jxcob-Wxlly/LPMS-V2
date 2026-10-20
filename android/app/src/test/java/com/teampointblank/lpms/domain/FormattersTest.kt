package com.teampointblank.lpms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test fun date_formatsIsoDate() {
        assertEquals("Fri, 16 Oct 2026", Formatters.date("2026-10-16"))
        assertEquals("16 Oct 2026", Formatters.shortDate("2026-10-16"))
    }

    @Test fun date_badInput_fallsBackToRawText() {
        assertEquals("not a date", Formatters.date("not a date"))
        assertEquals("", Formatters.date(null))
        assertNull(Formatters.parseDate(""))
        assertNull(Formatters.parseDate("2026-99-99"))
    }

    @Test fun time_formatting() {
        assertEquals("09:00", Formatters.time("09:00:00"))
        assertEquals("09:00 - 10:30", Formatters.timeRange("09:00:00", "10:30:00"))
        assertEquals("oops", Formatters.time("oops"))
    }

    @Test fun percent_andScore() {
        assertEquals("66.7%", Formatters.percent(66.67))
        assertEquals("85%", Formatters.percent(85.0))
        assertEquals("-", Formatters.percent(null))
        assertEquals("72.5", Formatters.score(72.5))
        assertEquals("80", Formatters.score(80.0))
        assertEquals("-", Formatters.score(null))
    }

    @Test fun names_andInitials() {
        assertEquals("Thandi Mokoena", Formatters.fullName("Thandi", "Mokoena"))
        assertEquals("TM", Formatters.initials("thandi", "Mokoena"))
        assertEquals("?", Formatters.initials("", ""))
    }

    @Test fun greeting_byHour() {
        assertEquals("Good morning", Formatters.greeting(8))
        assertEquals("Good afternoon", Formatters.greeting(13))
        assertEquals("Good evening", Formatters.greeting(19))
    }

    @Test fun plural_words() {
        assertEquals("1 session", Formatters.plural(1, "session"))
        assertEquals("0 sessions", Formatters.plural(0, "session"))
        assertEquals("3 learners", Formatters.plural(3, "learner"))
    }

    @Test fun weekdayDate_shows_day_and_month() {
        assertEquals("Mon 19 Oct", Formatters.weekdayDate("2026-10-19"))
    }

    @Test fun weekdayDate_falls_back_to_the_text() {
        assertEquals("not a date", Formatters.weekdayDate("not a date"))
    }
}
