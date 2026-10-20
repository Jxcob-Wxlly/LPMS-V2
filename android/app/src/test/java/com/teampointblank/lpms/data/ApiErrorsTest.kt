package com.teampointblank.lpms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorsTest {

    @Test fun problemDetails_detailIsUsed() {
        val body = """{"title":"Sign-in failed","status":401,"detail":"Incorrect email/username or password."}"""
        assertEquals("Incorrect email/username or password.", ApiErrors.parseProblemMessage(body))
    }

    @Test fun validationProblem_firstFieldMessageIsUsed() {
        val body = """{"title":"One or more validation errors occurred.","status":400,
            "errors":{"Identifier":["Enter your email address or username."],"Password":["Enter your password."]}}"""
        assertEquals("Enter your email address or username.", ApiErrors.parseProblemMessage(body))
    }

    @Test fun emptyOrNonJsonBodies_giveNull() {
        assertNull(ApiErrors.parseProblemMessage(null))
        assertNull(ApiErrors.parseProblemMessage(""))
        assertNull(ApiErrors.parseProblemMessage("<html>Bad gateway</html>"))
    }

    @Test fun statusCodes_mapToKinds() {
        assertEquals(ErrorKind.Validation, ApiErrors.kindFor(400))
        assertEquals(ErrorKind.Unauthorized, ApiErrors.kindFor(401))
        assertEquals(ErrorKind.Forbidden, ApiErrors.kindFor(403))
        assertEquals(ErrorKind.NotFound, ApiErrors.kindFor(404))
        assertEquals(ErrorKind.Conflict, ApiErrors.kindFor(409))
        assertEquals(ErrorKind.RateLimited, ApiErrors.kindFor(429))
        assertEquals(ErrorKind.Server, ApiErrors.kindFor(500))
        assertEquals(ErrorKind.Server, ApiErrors.kindFor(503))
        assertEquals(ErrorKind.Unknown, ApiErrors.kindFor(418))
    }

    @Test fun messages_areFriendly() {
        assertTrue(ApiErrors.messageFor(ErrorKind.Network, null).startsWith("Can't reach the server"))
        assertEquals(
            "Your session has expired. Please sign in again.",
            ApiErrors.messageFor(ErrorKind.Unauthorized, null)
        )
        assertEquals("Custom message.", ApiErrors.messageFor(ErrorKind.Validation, "Custom message."))
        // A 500 never shows raw server text.
        assertEquals(
            "The server ran into a problem. Please try again in a moment.",
            ApiErrors.messageFor(ErrorKind.Server, "System.NullReferenceException at ...")
        )
    }
}
