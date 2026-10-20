package com.teampointblank.lpms.data

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Outcome of an API call. The app never lets an exception escape to the UI; it gets one of these instead. */
sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Failure(val kind: ErrorKind, val message: String) : ApiResult<Nothing>
}

/** Runs [block] and converts every failure (HTTP error, no network, bad JSON, ...) into [ApiResult.Failure]. */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val body = try {
            e.response()?.errorBody()?.string()
        } catch (io: Exception) {
            null
        }
        val kind = ApiErrors.kindFor(e.code())
        ApiResult.Failure(kind, ApiErrors.messageFor(kind, ApiErrors.parseProblemMessage(body)))
    } catch (e: SocketTimeoutException) {
        ApiResult.Failure(ErrorKind.Timeout, ApiErrors.messageFor(ErrorKind.Timeout, null))
    } catch (e: IOException) {
        ApiResult.Failure(ErrorKind.Network, ApiErrors.messageFor(ErrorKind.Network, null))
    } catch (e: JsonParseException) {
        ApiResult.Failure(ErrorKind.Unknown, "The server sent a reply the app couldn't read.")
    } catch (e: Exception) {
        ApiResult.Failure(ErrorKind.Unknown, ApiErrors.messageFor(ErrorKind.Unknown, null))
    }
}
