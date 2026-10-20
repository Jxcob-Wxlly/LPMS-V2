package com.teampointblank.lpms.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the app's models read the JSON that the ASP.NET Core API actually sends (camelCase, ISO dates). */
class ApiModelsTest {
    private val gson = Gson()

    @Test fun loginResponse_parses() {
        val json = """{"token":"abc.def.ghi","expiresAtUtc":"2026-10-16T14:00:00.1234567Z",
            "user":{"userId":3,"username":"teacher01","fullName":"Thandi Mokoena","email":"thandi@read.org.za",
            "role":"Teacher","teacherId":1,"schoolId":2,"schoolName":"Sunrise Primary"}}"""
        val r = gson.fromJson(json, LoginResponse::class.java)
        assertEquals("abc.def.ghi", r.token)
        assertEquals("Teacher", r.user.role)
        assertEquals(1, r.user.teacherId)
        assertEquals("Sunrise Primary", r.user.schoolName)
    }

    @Test fun sessionSummary_parses_includingBooleans() {
        val json = """{"sessionId":7,"programmeId":1,"programmeName":"Reading Club","schoolId":2,"schoolName":"Sunrise",
            "sessionDate":"2026-10-16","startTime":"09:00:00","endTime":"10:00:00","status":"Pending",
            "isToday":true,"attendanceRecorded":false,"totalLearners":12,"presentCount":0,"absentCount":0}"""
        val s = gson.fromJson(json, SessionSummary::class.java)
        assertEquals(7, s.sessionId)
        assertTrue(s.isToday)
        assertFalse(s.attendanceRecorded)
        assertEquals("09:00:00", s.startTime)
        assertEquals(12, s.totalLearners)
    }

    @Test fun learnerProgress_parses_nullsAndNumbers() {
        val json = """{"learner":{"learnerId":5,"firstName":"Amahle","lastName":"Dlamini","grade":"Grade 3","schoolId":2,
            "schoolName":"Sunrise","averageScore":72.5,"attendanceRate":null,"assessmentCount":2},
            "progressLabel":"Good progress",
            "assessments":[{"progressId":9,"assessmentScore":72.5,"assessmentDate":"2026-10-01","notes":null,"recordedBy":"Thandi Mokoena","canEdit":true}],
            "attendance":[{"sessionId":1,"sessionDate":"2026-10-02","programmeName":"Reading Club","status":"Present"}]}"""
        val p = gson.fromJson(json, LearnerProgress::class.java)
        assertEquals(72.5, p.learner.averageScore!!, 0.0001)
        assertNull(p.learner.attendanceRate)
        assertEquals(1, p.assessments.size)
        assertNull(p.assessments[0].notes)
        assertTrue(p.assessments[0].canEdit)
        assertEquals("Present", p.attendance[0].status)
    }

    @Test fun missingFields_neverCrash() {
        val d = gson.fromJson("{}", TeacherDashboard::class.java)
        assertEquals(0, d.sessionsToday)
        assertTrue(d.todaySessions.isEmpty())
    }

    @Test fun requests_serialiseToTheNamesTheApiExpects() {
        val json = gson.toJson(CreateAssessmentRequest(5, 72.5, "2026-10-01", null))
        assertEquals("""{"learnerId":5,"assessmentScore":72.5,"assessmentDate":"2026-10-01"}""", json)
        val att = gson.toJson(SaveAttendanceRequest(listOf(AttendanceEntry(1, "Present"))))
        assertEquals("""{"entries":[{"learnerId":1,"status":"Present"}]}""", att)
        val login = gson.toJson(LoginRequest("teacher01", "pw"))
        assertEquals("""{"identifier":"teacher01","password":"pw"}""", login)
    }
}
