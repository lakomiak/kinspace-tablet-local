package com.adhdfocus.app.data.network

import com.google.gson.Gson
import retrofit2.Response

/**
 * Utility for handling API errors and extracting error messages
 */
object ErrorHandler {
    private val gson = Gson()

    /**
     * Extract error message from API response
     */
    fun getErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                errorResponse.message ?: errorResponse.error ?: "Unknown error"
            } else {
                when (response.code()) {
                    400 -> "Bad request"
                    401 -> "Unauthorized - please sign in again"
                    403 -> "Forbidden"
                    404 -> "Not found"
                    500 -> "Server error"
                    503 -> "Service unavailable"
                    else -> "Error: ${response.code()}"
                }
            }
        } catch (e: Exception) {
            "Error: ${response.code()}"
        }
    }

    /**
     * Extract error message from exception
     */
    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is java.net.SocketTimeoutException -> "Connection timeout"
            is java.net.ConnectException -> "Connection failed"
            is java.io.IOException -> "Network error"
            else -> exception.message ?: "Unknown error"
        }
    }
}
