package com.teampointblank.lpms.data

import com.google.gson.Gson

enum class ErrorKind {
    Network,
    Timeout,
    Unauthorized,
    Forbidden,
    NotFound,
    Validation,
    Conflict,
    RateLimited,
    Server,
    Unknown
}

/** Shape of ASP.NET Core ProblemDetails / ValidationProblemDetails responses. */
data class ProblemBody(
    val title: String? = null,
    val detail: String? = null,
    val status: Int? = null,
    val errors: Map<String, List<String>>? = null
)

/** Turns API failures into short, friendly messages. Pure Kotlin so it can be unit tested. */
object ApiErrors {
    private val gson = Gson()

    /** Pulls the human-readable message out of an error body, or null when there is none. */
    fun parseProblemMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val problem = gson.fromJson(body, ProblemBody::class.java) ?: return null
            val detail = problem.detail
            if (!detail.isNullOrBlank()) return detail
            val firstValidation = problem.errors?.values
                ?.flatten()
                ?.firstOrNull { !it.isNullOrBlank() }
            firstValidation
        } catch (e: Exception) {
            null
        }
    }

    fun kindFor(statusCode: Int): ErrorKind = when (statusCode) {
        400, 422 -> ErrorKind.Validation
        401 -> ErrorKind.Unauthorized
        403 -> ErrorKind.Forbidden
        404 -> ErrorKind.NotFound
        409 -> ErrorKind.Conflict
        429 -> ErrorKind.RateLimited
        in 500..599 -> ErrorKind.Server
        else -> ErrorKind.Unknown
    }

    fun messageFor(kind: ErrorKind, serverMessage: String?): String = when (kind) {
        ErrorKind.Network ->
            "Can't reach the server. Check your internet connection and the server address in Settings."
        ErrorKind.Timeout ->
            "The server took too long to respond. It may be waking up - please try again in a moment."
        ErrorKind.Unauthorized -> serverMessage ?: "Your session has expired. Please sign in again."
        ErrorKind.Forbidden -> serverMessage ?: "You don't have permission to do that."
        ErrorKind.NotFound -> serverMessage ?: "We couldn't find that. It may have been removed."
        ErrorKind.Validation -> serverMessage ?: "Some of the information entered isn't valid."
        ErrorKind.Conflict -> serverMessage ?: "That clashes with a recent change. Reload and try again."
        ErrorKind.RateLimited -> "Too many attempts. Please wait a minute and try again."
        ErrorKind.Server -> "The server ran into a problem. Please try again in a moment."
        ErrorKind.Unknown -> serverMessage ?: "Something went wrong. Please try again."
    }
}
