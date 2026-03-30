package com.adhdfocus.app.domain.sync

/**
 * Interface for defining retry behavior for failed sync attempts.
 *
 * Responsibilities:
 * - Determine if a failed operation should be retried
 * - Calculate backoff delay between retries
 * - Define maximum retry attempts
 * - Detect transient errors that warrant retry
 */
interface RetryPolicy {
    /**
     * Determines if an operation should be retried based on the attempt number and exception.
     *
     * @param attempt Current attempt number (0-indexed)
     * @param exception The exception that caused the failure
     * @return true if the operation should be retried, false otherwise
     */
    fun shouldRetry(attempt: Int, exception: Exception): Boolean

    /**
     * Calculates the backoff delay in milliseconds for the given attempt.
     *
     * @param attempt Current attempt number (0-indexed)
     * @return Delay in milliseconds before the next retry
     */
    fun getBackoffDelayMs(attempt: Int): Long

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return Maximum number of retries
     */
    fun getMaxRetries(): Int
}
