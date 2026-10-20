package com.teampointblank.lpms.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TokenRulesTest {
    private val now = Instant.parse("2026-10-16T11:00:00Z")

    @Test fun pastExpiry_isExpired() {
        assertTrue(TokenRules.isExpired("2026-10-16T10:00:00Z", now))
        assertTrue(TokenRules.isExpired("2026-10-16T10:59:59.1234567Z", now))
    }

    @Test fun futureExpiry_isNotExpired() {
        assertFalse(TokenRules.isExpired("2026-10-16T12:00:00Z", now))
    }

    @Test fun unreadableExpiry_isTreatedAsNotExpired() {
        assertFalse(TokenRules.isExpired("", now))
        assertFalse(TokenRules.isExpired("tomorrow", now))
    }
}
