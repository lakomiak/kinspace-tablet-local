package com.adhdfocus.app.domain.sync

import kotlin.math.min
import kotlin.random.Random

/**
 * Implementation of RetryPolicy using exponential backoff with jitter.
 *
 * Backoff calculation:
 * - Initial delay: 100ms
 * - Multiplier: 2.0 (doubles each retry)
 * - Max delay: 32s
 * - Max retries: 5
 * - Jitter: ±10% to prevent thundering herd
 *
 * Transient errors (retryable):
 * - NetworkException
 * - ApiException with 5xx status codes (server errors)
 * - ApiException with 429 status code (rate limiting)
 * - ApiException with 408 status code (request timeout)
 *
 * Non-transient errors (not retryable):
 * - ApiException with 4xx status codes (client errors, except 408, 429)
 * - Other exceptions
 */
class ExponentialBackoffRetryPolicy(
    private val initialBackoffMs: Long = 100L,
    private val maxBackoffMs: Long = 32000L,
    private val multiplier: Double = 2.0,
    private val maxRetries: Int = 5,
    private val jitterFraction: Double = 0.1
) : RetryPolicy {

    override fun shouldRetry(attempt: Int, exception: Exception): Boolean {
        // Don't retry if we've exhausted attempts
        if (attempt >= maxRetries) {
            return false
        }

        return isTransientError(exception)
    }

    override fun getBackoffDelayMs(attempt: Int): Long {
        // Calculate exponential backoff: initialBackoff * (multiplier ^ attempt)
        val exponentialDelay = (initialBackoffMs * Math.pow(multiplier, attempt.toDouble())).toLong()
        val cappedDelay = min(exponentialDelay, maxBackoffMs)

        // Add jitter: ±jitterFraction of the delay
        val jitterRange = (cappedDelay * jitterFraction).toLong()
        val jitter = Random.nextLong(-jitterRange, jitterRange + 1)

        return (cappedDelay + jitter).coerceAtLeast(0L)
    }

    override fun getMaxRetries(): Int = maxRetries

    /**
     * Determines if an exception represents a transient error that should be retried.
     *
     * @param exception The exception to check
     * @return true if the error is transient and should be retried
     */
    private fun isTransientError(exception: Exception): Boolean {
        return when (exception) {
            is NetworkException -> true
            is ApiException -> isTransientApiError(exception.code)
            else -> false
        }
    }

    /**
     * Determines if an HTTP status code represents a transient error.
     *
     * @param statusCode HTTP status code
     * @return true if the status code indicates a transient error
     */
    private fun isTransientApiError(statusCode: Int): Boolean {
        return when {
            statusCode >= 500 -> true  // Server errors (5xx)
            statusCode == 429 -> true  // Rate limiting
            statusCode == 408 -> true  // Request timeout
            else -> false
        }
    }
}
