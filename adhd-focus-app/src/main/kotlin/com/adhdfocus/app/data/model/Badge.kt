package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val userId: String,
    val badgeType: String,
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val earnedAt: Long = System.currentTimeMillis(),
    val seasonYear: Int = java.time.LocalDate.now().year,
    val progress: Int? = null,
    val isLocked: Boolean = true
)
