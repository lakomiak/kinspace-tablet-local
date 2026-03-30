package com.adhdfocus.app.data.network

import com.adhdfocus.app.data.security.TokenStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private lateinit var tokenStorage: TokenStorage
    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setup() {
        tokenStorage = mockk()
        interceptor = AuthInterceptor(tokenStorage)
    }

    @Test
    fun `interceptor adds authorization header when token exists`() {
        // Arrange
        val token = "access_token_123"
        every { tokenStorage.getAccessToken() } returns token

        val request = Request.Builder()
            .url("https://api.example.com/api/tasks")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        // Act
        interceptor.intercept(chain)

        // Assert - Verify that proceed was called with a request containing the auth header
        val capturedRequest = io.mockk.slot<Request>()
        io.mockk.verify { chain.proceed(capture(capturedRequest)) }
        assert(capturedRequest.captured.header("Authorization") == "Bearer $token")
    }

    @Test
    fun `interceptor skips auth endpoints`() {
        // Arrange
        val token = "access_token_123"
        every { tokenStorage.getAccessToken() } returns token

        val request = Request.Builder()
            .url("https://api.example.com/api/auth/login")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        // Act
        interceptor.intercept(chain)

        // Assert - Verify that proceed was called with original request (no auth header)
        val capturedRequest = io.mockk.slot<Request>()
        io.mockk.verify { chain.proceed(capture(capturedRequest)) }
        assert(capturedRequest.captured.header("Authorization") == null)
    }

    @Test
    fun `interceptor proceeds without token when none exists`() {
        // Arrange
        every { tokenStorage.getAccessToken() } returns null

        val request = Request.Builder()
            .url("https://api.example.com/api/tasks")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        // Act
        interceptor.intercept(chain)

        // Assert - Verify that proceed was called with original request
        val capturedRequest = io.mockk.slot<Request>()
        io.mockk.verify { chain.proceed(capture(capturedRequest)) }
        assert(capturedRequest.captured.header("Authorization") == null)
    }
}
