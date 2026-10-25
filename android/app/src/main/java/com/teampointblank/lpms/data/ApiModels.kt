package com.teampointblank.lpms.data

// JSON contract of the LPMS API (ASP.NET Core, camelCase). Every field has a default so a missing
// field can never crash the app. Dates are "yyyy-MM-dd", times "HH:mm:ss".

data class LoginRequest(val identifier: String, val password: String)

data class UserProfile(
    val userId: Int = 0,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val teacherId: Int? = null,
    val schoolId: Int? = null,
    val schoolName: String? = null
)

data class LoginResponse(
    val token: String = "",
    val expiresAtUtc: String = "",
    val user: UserProfile = UserProfile()
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class SessionSummary(
    val sessionId: Int = 0,
    val programmeId: Int = 0,
    val programmeName: String = "",
    val schoolId: Int = 0,
    val schoolName: String = "",
    val sessionDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val isToday: Boolean = false,
    val attendanceRecorded: Boolean = false,
    val totalLearners: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0
)

data class RosterEntry(
    val learnerId: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val grade: String = "",
    val status: String? = null
)

data class SessionDetail(
    val session: SessionSummary = SessionSummary(),
    val learners: List<RosterEntry> = emptyList()
)

data class AttendanceEntry(val learnerId: Int, val status: String)

data class SaveAttendanceRequest(val entries: List<AttendanceEntry>)

data class LearnerSummary(
    val learnerId: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val grade: String = "",
    val schoolId: Int = 0,
    val schoolName: String = "",
    val averageScore: Double? = null,
    val attendanceRate: Double? = null,
    val assessmentCount: Int = 0
)

data class Assessment(
    val progressId: Int = 0,
    val assessmentScore: Double = 0.0,
    val assessmentDate: String = "",
    val notes: String? = null,
    val recordedBy: String = "",
    val canEdit: Boolean = false
)

data class LearnerAttendance(
    val sessionId: Int = 0,
    val sessionDate: String = "",
    val programmeName: String = "",
    val status: String = ""
)

data class LearnerProgress(
    val learner: LearnerSummary = LearnerSummary(),
    val progressLabel: String = "",
    val assessments: List<Assessment> = emptyList(),
    val attendance: List<LearnerAttendance> = emptyList()
)

data class CreateAssessmentRequest(
    val learnerId: Int,
    val assessmentScore: Double,
    val assessmentDate: String,
    val notes: String?
)

data class UpdateAssessmentRequest(
    val assessmentScore: Double,
    val assessmentDate: String,
    val notes: String?
)

data class TeacherDashboard(
    val teacherName: String = "",
    val schoolName: String = "",
    val sessionsThisWeek: Int = 0,
    val sessionsToday: Int = 0,
    val totalLearners: Int = 0,
    val attendanceRate: Double? = null,
    val pendingAttendance: Int = 0,
    val todaySessions: List<SessionSummary> = emptyList()
)

data class HealthInfo(
    val status: String = "",
    val database: String = "",
    val timeUtc: String = "",
    val version: String = ""
)

data class SchoolOption(val schoolId: Int = 0, val schoolName: String = "")

data class RegisterRequest(val fullName: String, val email: String, val schoolId: Int, val password: String)

data class Programme(
    val programmeId: Int = 0,
    val programmeName: String = "",
    val description: String? = null,
    val startDate: String = "",
    val endDate: String = ""
)

data class CreateSessionRequest(
    val programmeId: Int,
    val sessionDate: String,
    val startTime: String,
    val endTime: String
)

data class ConversationSummary(
    val learnerId: Int = 0,
    val learnerName: String = "",
    val grade: String = "",
    val parentUserId: Int = 0,
    val parentName: String = "",
    val schoolName: String = "",
    val lastMessage: String? = null,
    val lastSentAt: String? = null,
    val lastSenderName: String? = null,
    val messageCount: Int = 0
)

data class ChatMessage(
    val messageId: Int = 0,
    val senderUserId: Int = 0,
    val senderName: String = "",
    val senderRole: String = "",
    val body: String = "",
    val sentAt: String = "",
    val isMine: Boolean = false
)

data class SendMessageRequest(val body: String)

data class Announcement(
    val announcementId: Int = 0,
    val title: String = "",
    val body: String = "",
    val audience: String = "",
    val schoolId: Int? = null,
    val schoolName: String? = null,
    val authorName: String = "",
    val createdAt: String = ""
)
