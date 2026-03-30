package com.adhdfocus.app.data.network

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

class ErrorHandlerTest {

    @Test
    fun `getErrorMessage extracts message from error response`() {
        // Arrange
        val errorJson = """{"error":"invalid_credentials","message":"Invalid email or password","code":401}"""
        val errorBody = errorJson.toResponseBody()
        val response = Response.error<String>(401, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Invalid email or password")
    }

    @Test
    fun `getErrorMessage falls back to error field when message is missing`() {
        // Arrange
        val errorJson = """{"error":"unauthorized","code":401}"""
        val errorBody = errorJson.toResponseBody()
        val response = Response.error<String>(401, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "unauthorized")
    }

    @Test
    fun `getErrorMessage returns default message for 400 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(400, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Bad request")
    }

    @Test
    fun `getErrorMessage returns default message for 401 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(401, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message.contains("Unauthorized"))
    }

    @Test
    fun `getErrorMessage returns default message for 403 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(403, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Forbidden")
    }

    @Test
    fun `getErrorMessage returns default message for 404 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(404, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Not found")
    }

    @Test
    fun `getErrorMessage returns default message for 500 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(500, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Server error")
    }

    @Test
    fun `getErrorMessage returns default message for 503 error`() {
        // Arrange
        val errorBody = "".toResponseBody()
        val response = Response.error<String>(503, errorBody)

        // Act
        val message = ErrorHandler.getErrorMessage(response)

        // Assert
        assert(message == "Service unavailable")
    }

    @Test
    fun `getErrorMessage from exception handles socket timeout`() {
        // Arrange
        val exception = java.net.SocketTimeoutException("Connection timeout")

        // Act
        val message = ErrorHandler.getErrorMessage(exception)

        // Assert
        assert(message == "Connection timeout")
    }

    @Test
    fun `getErrorMessage from exception handles connection error`() {
        // Arrange
        val exception = java.net.ConnectException("Connection refused")

        // Act
        val message = ErrorHandler.getErrorMessage(exception)

        // Assert
        assert(message == "Connection failed")
    }

    @Test
    fun `getErrorMessage from exception handles IO error`() {
        // Arrange
        val exception = java.io.IOException("Network error")

        // Act
        val message = ErrorHandler.getErrorMessage(exception)

        // Assert
        assert(message == "Network error")
    }

    @Test
    fun `getErrorMessage from exception returns message when available`() {
        // Arrange
        val exception = Exception("Custom error message")

        // Act
        val message = ErrorHandler.getErrorMessage(exception)

        // Assert
        assert(message == "Custom error message")
    }

    @Test
    fun `getErrorMessage from exception returns default when message is null`() {
        // Arrange
        val exception = Exception()

        // Act
        val message = ErrorHandler.getErrorMessage(exception)

        // Assert
        assert(message == "Unknown error")
    }
}
