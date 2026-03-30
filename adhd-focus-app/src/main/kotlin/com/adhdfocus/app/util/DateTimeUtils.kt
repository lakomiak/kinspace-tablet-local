package com.adhdfocus.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility functions for date and time operations.
 */
object DateTimeUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")

    /**
     * Converts milliseconds to LocalDateTime.
     *
     * @param millis Milliseconds since epoch
     * @return LocalDateTime
     */
    fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
    }

    /**
     * Converts LocalDateTime to milliseconds.
     *
     * @param dateTime LocalDateTime
     * @return Milliseconds since epoch
     */
    fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Formats milliseconds as a date string.
     *
     * @param millis Milliseconds since epoch
     * @return Formatted date string
     */
    fun formatDate(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return dateTime.format(dateFormatter)
    }

    /**
     * Formats milliseconds as a time string.
     *
     * @param millis Milliseconds since epoch
     * @return Formatted time string
     */
    fun formatTime(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return dateTime.format(timeFormatter)
    }

    /**
     * Formats milliseconds as a date-time string.
     *
     * @param millis Milliseconds since epoch
     * @return Formatted date-time string
     */
    fun formatDateTime(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return dateTime.format(dateTimeFormatter)
    }

    /**
     * Checks if a timestamp is today.
     *
     * @param millis Milliseconds since epoch
     * @return True if timestamp is today
     */
    fun isToday(millis: Long): Boolean {
        val date = millisToLocalDateTime(millis).toLocalDate()
        return date == LocalDate.now()
    }

    /**
     * Checks if a timestamp is yesterday.
     *
     * @param millis Milliseconds since epoch
     * @return True if timestamp is yesterday
     */
    fun isYesterday(millis: Long): Boolean {
        val date = millisToLocalDateTime(millis).toLocalDate()
        return date == LocalDate.now().minusDays(1)
    }

    /**
     * Gets the start of day in milliseconds.
     *
     * @return Milliseconds at start of today
     */
    fun getStartOfDay(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Gets the end of day in milliseconds.
     *
     * @return Milliseconds at end of today
     */
    fun getEndOfDay(): Long {
        return LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
