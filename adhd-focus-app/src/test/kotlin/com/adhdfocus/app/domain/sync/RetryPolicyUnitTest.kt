package com.adhdfocus.app.domain.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual

class RetryPolicyUnitTest : FunSpec({
    val policy = ExponentialBackoffRetryPolicy()

    test("shouldRetry returns true for NetworkException within max retries") {
        val exception = NetworkException("Connection failed")
        
        policy.shouldRetry(0, exception) shouldBe true
        policy.shouldRetry(1, exception) shouldBe true
        policy.shouldRetry(4, exception) shouldBe true
    }

    test("shouldRetry returns false after max retries exceeded") {
        val exception = NetworkException("Connection failed")
        
        policy.shouldRetry(5, exception) shouldBe false
        policy.shouldRetry(6, exception) shouldBe false
    }

    test("shouldRetry returns true for 5xx API errors") {
        val exception500 = ApiException(500, "Internal Server Error")
        val exception503 = ApiException(503, "Service Unavailable")
        
        policy.shouldRetry(0, exception500) shouldBe true
        policy.shouldRetry(0, exception503) shouldBe true
    }

    test("shouldRetry returns true for 429 rate limiting") {
        val exception = ApiException(429, "Too Many Requests")
        
        policy.shouldRetry(0, exception) shouldBe true
    }

    test("shouldRetry returns true for 408 request timeout") {
        val exception = ApiException(408, "Request Timeout")
        
        policy.shouldRetry(0, exception) shouldBe true
    }

    test("shouldRetry returns false for 4xx client errors (except 408, 429)") {
        val exception400 = ApiException(400, "Bad Request")
        val exception401 = ApiException(401, "Unauthorized")
        val exception403 = ApiException(403, "Forbidden")
        val exception404 = ApiException(404, "Not Found")
        
        policy.shouldRetry(0, exception400) shouldBe false
        policy.shouldRetry(0, exception401) shouldBe false
        policy.shouldRetry(0, exception403) shouldBe false
        policy.shouldRetry(0, exception404) shouldBe false
    }

    test("shouldRetry returns false for non-transient exceptions") {
        val exception = IllegalArgumentException("Invalid argument")
        
        policy.shouldRetry(0, exception) shouldBe false
    }

    test("getBackoffDelayMs returns exponential delays") {
        val delay0 = policy.getBackoffDelayMs(0)
        val delay1 = policy.getBackoffDelayMs(1)
        val delay2 = policy.getBackoffDelayMs(2)
        
        // Delays should increase (accounting for jitter)
        delay0 shouldBeGreaterThanOrEqual 80L  // 100ms - 10% jitter
        delay0 shouldBeLessThanOrEqual 110L    // 100ms + 10% jitter
        
        delay1 shouldBeGreaterThanOrEqual 180L // 200ms - 10% jitter
        delay1 shouldBeLessThanOrEqual 220L    // 200ms + 10% jitter
        
        delay2 shouldBeGreaterThanOrEqual 360L // 400ms - 10% jitter
        delay2 shouldBeLessThanOrEqual 440L    // 400ms + 10% jitter
    }

    test("getBackoffDelayMs caps at max backoff") {
        val delay4 = policy.getBackoffDelayMs(4)
        
        // 100 * 2^4 = 1600ms, but should be capped at 32000ms
        // With jitter, should be around 1600ms
        delay4 shouldBeGreaterThanOrEqual 1440L  // 1600ms - 10% jitter
        delay4 shouldBeLessThanOrEqual 1760L     // 1600ms + 10% jitter
    }

    test("getBackoffDelayMs never exceeds max backoff") {
        repeat(10) { attempt ->
            val delay = policy.getBackoffDelayMs(attempt)
            delay shouldBeLessThanOrEqual 35200L  // 32000ms + 10% jitter
        }
    }

    test("getBackoffDelayMs never returns negative") {
        repeat(10) { attempt ->
            val delay = policy.getBackoffDelayMs(attempt)
            delay shouldBeGreaterThanOrEqual 0L
        }
    }

    test("getMaxRetries returns configured value") {
        policy.getMaxRetries() shouldBe 5
    }

    test("custom configuration works correctly") {
        val customPolicy = ExponentialBackoffRetryPolicy(
            initialBackoffMs = 50L,
            maxBackoffMs = 5000L,
            multiplier = 3.0,
            maxRetries = 3
        )
        
        customPolicy.getMaxRetries() shouldBe 3
        
        val delay0 = customPolicy.getBackoffDelayMs(0)
        delay0 shouldBeGreaterThanOrEqual 45L   // 50ms - 10% jitter
        delay0 shouldBeLessThanOrEqual 55L      // 50ms + 10% jitter
    }

    test("shouldRetry respects max retries with custom configuration") {
        val customPolicy = ExponentialBackoffRetryPolicy(maxRetries = 3)
        val exception = NetworkException("Connection failed")
        
        customPolicy.shouldRetry(0, exception) shouldBe true
        customPolicy.shouldRetry(1, exception) shouldBe true
        customPolicy.shouldRetry(2, exception) shouldBe true
        customPolicy.shouldRetry(3, exception) shouldBe false
    }

    test("shouldRetry with 503 Service Unavailable") {
        val exception = ApiException(503, "Service Unavailable")
        
        policy.shouldRetry(0, exception) shouldBe true
        policy.shouldRetry(1, exception) shouldBe true
    }

    test("shouldRetry with 502 Bad Gateway") {
        val exception = ApiException(502, "Bad Gateway")
        
        policy.shouldRetry(0, exception) shouldBe true
    }
})
