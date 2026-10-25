package com.teampointblank.lpms.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit description of the LPMS REST API (see docs/api-reference.md). */
interface LpmsApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/auth/schools")
    suspend fun schoolOptions(): List<SchoolOption>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("api/programmes")
    suspend fun programmes(): List<Programme>

    @POST("api/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionSummary

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    @GET("api/sessions/mine")
    suspend fun mySessions(@Query("scope") scope: String): List<SessionSummary>

    @GET("api/sessions/{id}")
    suspend fun session(@Path("id") id: Int): SessionDetail

    @PUT("api/sessions/{id}/attendance")
    suspend fun saveAttendance(@Path("id") id: Int, @Body body: SaveAttendanceRequest): SessionDetail

    @GET("api/learners")
    suspend fun learners(@Query("search") search: String?): List<LearnerSummary>

    @GET("api/parent/children")
    suspend fun parentChildren(): List<LearnerSummary>

    @GET("api/learners/{id}/progress")
    suspend fun learnerProgress(@Path("id") id: Int): LearnerProgress

    @POST("api/progress")
    suspend fun addAssessment(@Body body: CreateAssessmentRequest): Assessment

    @PUT("api/progress/{id}")
    suspend fun updateAssessment(@Path("id") id: Int, @Body body: UpdateAssessmentRequest): Assessment

    @DELETE("api/progress/{id}")
    suspend fun deleteAssessment(@Path("id") id: Int): Response<Unit>

    @GET("api/dashboard/teacher")
    suspend fun dashboard(): TeacherDashboard

    @GET("api/messages/conversations")
    suspend fun conversations(): List<ConversationSummary>

    @GET("api/messages/conversations/{learnerId}/{parentUserId}")
    suspend fun thread(@Path("learnerId") learnerId: Int, @Path("parentUserId") parentUserId: Int): List<ChatMessage>

    @POST("api/messages/conversations/{learnerId}/{parentUserId}")
    suspend fun sendMessage(
        @Path("learnerId") learnerId: Int,
        @Path("parentUserId") parentUserId: Int,
        @Body body: SendMessageRequest
    ): ChatMessage

    @GET("api/announcements")
    suspend fun announcements(): List<Announcement>

    @GET("api/health")
    suspend fun health(): HealthInfo
}
