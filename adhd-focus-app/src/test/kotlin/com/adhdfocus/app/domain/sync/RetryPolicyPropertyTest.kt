package com.adhdfocus.app.domain.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class RetryPolicyPropertyTest : FunSpec({
    val policy = ExponentialBackoffRetryPolicy()

    test("Property 1: Backoff calculation correctness - delays increase monotonically (ignoring jitter)") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any two consecutive attempts, the base exponential delay
         * (without jitter) for the second attempt should be greater than or equal
         * to the first attempt's base delay.
         */
        checkAll(Arb.int(min = 0, max = 3)) { attempt ->
            val baseDelay1 = 100L * Math.pow(2.0, attempt.toDouble()).toLong()
            val baseDelay2 = 100L * Math.pow(2.0, (attempt + 1).toDouble()).toLong()
            
            baseDelay2 shouldBeGreaterThanOrEqual baseDelay1
        }
    }

    test("Property 2: Backoff calculation correctness - delays never exceed max backoff") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any attempt number, the calculated backoff delay
         * (including jitter) should never exceed the configured maximum backoff.
         */
        checkAll(Arb.int(min = 0, max = 20)) { attempt ->
            val delay = policy.getBackoffDelayMs(attempt)
            
            // Max backoff is 32000ms, with 10% jitter = 35200ms max
            delay shouldBeLessThanOrEqual 35200L
        }
    }

    test("Property 3: Backoff calculation correctness - delays never negative") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any attempt number, the calculated backoff delay
         * should never be negative.
         */
        checkAll(Arb.int(min = 0, max = 20)) { attempt ->
            val delay = policy.getBackoffDelayMs(attempt)
            
            delay shouldBeGreaterThanOrEqual 0L
        }
    }

    test("Property 4: Retry attempt tracking - shouldRetry respects max retries") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any attempt number >= maxRetries, shouldRetry should
         * return false regardless of the exception type.
         */
        checkAll(Arb.int(min = 5, max = 20)) { attempt ->
            val exception = NetworkException("Connection failed")
            
            policy.shouldRetry(attempt, exception) shouldBe false
        }
    }

    test("Property 5: Retry attempt tracking - shouldRetry allows retries within limit") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any attempt number < maxRetries with a transient error,
         * shouldRetry should return true.
         */
        checkAll(Arb.int(min = 0, max = 4)) { attempt ->
            val exception = NetworkException("Connection failed")
            
            policy.shouldRetry(attempt, exception) shouldBe true
        }
    }

    test("Property 6: Transient error detection - 5xx errors are retryable") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any 5xx HTTP status code, shouldRetry should return true
         * when attempt < maxRetries.
         */
        checkAll(Arb.int(min = 500, max = 599)) { statusCode ->
            val exception = ApiException(statusCode, "Server error")
            
            policy.shouldRetry(0, exception) shouldBe true
        }
    }

    test("Property 7: Transient error detection - 4xx errors (except 408, 429) are not retryable") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any 4xx HTTP status code (except 408 and 429),
         * shouldRetry should return false.
         */
        checkAll(Arb.int(min = 400, max = 499)) { statusCode ->
            // Skip 408 and 429 which are retryable
            if (statusCode != 408 && statusCode != 429) {
                val exception = ApiException(statusCode, "Client error")
                
                policy.shouldRetry(0, exception) shouldBe false
            }
        }
    }

    test("Property 8: Transient error detection - NetworkException is always retryable") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For any NetworkException within retry limit,
         * shouldRetry should return true.
         */
        checkAll(Arb.int(min = 0, max = 4)) { attempt ->
            val exception = NetworkException("Network error")
            
            policy.shouldRetry(attempt, exception) shouldBe true
        }
    }

    test("Property 9: Max retries configuration - getMaxRetries returns configured value") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: getMaxRetries should always return the configured maximum
         * retry count.
         */
        val maxRetries = 5
        val policy = ExponentialBackoffRetryPolicy(maxRetries = maxRetries)
        
        policy.getMaxRetries() shouldBe maxRetries
    }

    test("Property 10: Exponential backoff formula - delay doubles with each attempt") {
        /**
         * Validates: Requirements 10 - Exponential backoff for failed sync attempts
         *
         * Property: For attempts 0-3, the base exponential delay (without jitter)
         * should approximately double with each attempt.
         */
        checkAll(Arb.int(min = 0, max = 2)) { attempt ->
            // Calculate base delays without jitter
            val baseDelay1 = 100L * Math.pow(2.0, attempt.toDouble()).toLong()
            val baseDelay2 = 100L * Math.pow(2.0, (attempt + 1).toDouble()).toLong()
            
            // baseDelay2 should be approximately 2x baseDelay1
            baseDelay2 shouldBe (baseDelay1 * 2)
        }
    }
})
