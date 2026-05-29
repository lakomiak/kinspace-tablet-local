package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [
        Index("householdId"),
        Index("email"),
        Index("role"),
        Index(value = ["householdId", "role"])
    ]
)
data class User(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val birthDate: LocalDate? = null,
    val role: UserRole = UserRole.ADHD_USER,
    val isPinProtected: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        require(email.isNotBlank()) { "email cannot be blank" }
        require(displayName.isNotBlank()) { "displayName cannot be blank" }
        require(email.contains("@")) { "email must be a valid email address" }
    }
}

enum class UserRole {
    ADHD_USER,
    CAREGIVER,
    ADMIN
}
