package com.adhdfocus.app.domain.reminder

import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TodoCategoryReminder(
    val groupName: String,
    val defaultEndTime: LocalTime
) {
    MORNING("Morning", LocalTime.of(12, 0)),
    AFTERNOON("Afternoon", LocalTime.of(17, 0)),
    EVENING("Evening", LocalTime.of(20, 0)),
    BEDTIME("Bedtime", LocalTime.of(22, 0));

    val displayName: String
        get() = groupName

    val endTimeLabel: String
        get() = defaultEndTime.format(DISPLAY_TIME_FORMATTER)

    companion object {
        private val DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

        fun fromGroupName(groupName: String): TodoCategoryReminder? {
            return values().firstOrNull { it.groupName.equals(groupName, ignoreCase = true) }
        }
    }
}
