package com.teampointblank.lpms.data

import retrofit2.HttpException

/** The one place the UI talks to the API. Every function returns an [ApiResult]; none of them throws. */
class LpmsRepository(
    private val provider: ApiProvider,
    private val auth: AuthManager
) {

    suspend fun signIn(identifier: String, password: String): ApiResult<UserProfile> {
        val result = safeApiCall { provider.api().login(LoginRequest(identifier.trim(), password)) }
        return when (result) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val response = result.data
                when {
                    response.token.isBlank() ->
                        ApiResult.Failure(ErrorKind.Unknown, "The server did not return a sign-in token.")
                    !isAppRole(response.user.role) ->
                        ApiResult.Failure(
                            ErrorKind.Forbidden,
                            "This app is for teachers and parents. Administrators and coordinators use the web portal."
                        )
                    else -> {
                        auth.signIn(response)
                        ApiResult.Success(response.user)
                    }
                }
            }
        }
    }

    suspend fun schoolOptions(): ApiResult<List<SchoolOption>> = safeApiCall { provider.api().schoolOptions() }

    /** Creates a teacher account and signs the new teacher straight in. */
    suspend fun register(fullName: String, email: String, schoolId: Int, password: String): ApiResult<UserProfile> {
        val result = safeApiCall {
            provider.api().register(RegisterRequest(fullName.trim(), email.trim(), schoolId, password))
        }
        return when (result) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                if (result.data.token.isBlank()) {
                    ApiResult.Failure(ErrorKind.Unknown, "The server did not return a sign-in token.")
                } else {
                    auth.signIn(result.data)
                    ApiResult.Success(result.data.user)
                }
            }
        }
    }

    suspend fun programmes(): ApiResult<List<Programme>> = authed { it.programmes() }

    suspend fun createSession(request: CreateSessionRequest): ApiResult<SessionSummary> =
        authed { it.createSession(request) }

    fun signOut() = auth.signOut()

    suspend fun changePassword(current: String, new: String): ApiResult<Unit> = authed { api ->
        val response = api.changePassword(ChangePasswordRequest(current, new))
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun children(): ApiResult<List<LearnerSummary>> = authed { it.parentChildren() }

    suspend fun dashboard(): ApiResult<TeacherDashboard> = authed { it.dashboard() }

    suspend fun sessions(scope: String): ApiResult<List<SessionSummary>> = authed { it.mySessions(scope) }

    suspend fun session(id: Int): ApiResult<SessionDetail> = authed { it.session(id) }

    suspend fun saveAttendance(id: Int, entries: List<AttendanceEntry>): ApiResult<SessionDetail> =
        authed { it.saveAttendance(id, SaveAttendanceRequest(entries)) }

    suspend fun learners(search: String?): ApiResult<List<LearnerSummary>> = authed { it.learners(search) }

    suspend fun learnerProgress(id: Int): ApiResult<LearnerProgress> = authed { it.learnerProgress(id) }

    suspend fun addAssessment(
        learnerId: Int,
        score: Double,
        dateIso: String,
        notes: String?
    ): ApiResult<Assessment> = authed {
        it.addAssessment(CreateAssessmentRequest(learnerId, score, dateIso, notes))
    }

    suspend fun updateAssessment(
        id: Int,
        score: Double,
        dateIso: String,
        notes: String?
    ): ApiResult<Assessment> = authed {
        it.updateAssessment(id, UpdateAssessmentRequest(score, dateIso, notes))
    }

    suspend fun deleteAssessment(id: Int): ApiResult<Unit> = authed { api ->
        val response = api.deleteAssessment(id)
        if (!response.isSuccessful) throw HttpException(response)
    }

    /** Anonymous health check, used by Settings > "Test connection". */
    suspend fun checkServer(): ApiResult<HealthInfo> = safeApiCall { provider.api().health() }

    private suspend fun <T> authed(block: suspend (LpmsApi) -> T): ApiResult<T> {
        val result = safeApiCall { block(provider.api()) }
        if (result is ApiResult.Failure && result.kind == ErrorKind.Unauthorized) {
            auth.signOut("Your session has expired. Please sign in again.")
        }
        return result
    }

    private companion object {
        const val TEACHER_ROLE = "Teacher"
        const val PARENT_ROLE = "Parent"

        fun isAppRole(role: String) =
            role.equals(TEACHER_ROLE, ignoreCase = true) || role.equals(PARENT_ROLE, ignoreCase = true)
    }
}
