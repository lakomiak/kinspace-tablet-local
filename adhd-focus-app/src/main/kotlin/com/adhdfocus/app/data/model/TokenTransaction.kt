package com.adhdfocus.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "token_transactions",
    indices = [
        Index("householdId"),
        Index("userId"),
        Index("type"),
        Index("createdAt"),
        Index(value = ["householdId", "userId"]),
        Index(value = ["householdId", "userId", "createdAt"]),
        Index(value = ["householdId", "userId", "taskId", "targetDate"], unique = true)
    ]
)
data class TokenTransaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String,
    val userId: String,
    val amount: Int,
    val type: TokenTransactionType,
    val note: String? = null,
    val taskId: String? = null,
    val targetDate: String? = null,
    val createdAt: Instant = Instant.now(),
    val createdBy: String? = null
) {
    init {
        require(householdId.isNotBlank()) { "householdId cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(amount != 0) { "amount cannot be zero" }
    }
}

enum class TokenTransactionType {
    TASK_AWARD,
    PARENT_ADJUSTMENT,
    REDEMPTION
}
