package com.adhdfocus.app.data.repository

import com.adhdfocus.app.data.dao.TokenTransactionDao
import com.adhdfocus.app.data.model.TokenTransaction
import com.adhdfocus.app.data.model.TokenTransactionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class TokenRepository @Inject constructor(
    private val tokenTransactionDao: TokenTransactionDao
) {
    fun observeTransactionsForUser(householdId: String, userId: String): Flow<List<TokenTransaction>> {
        return tokenTransactionDao.observeTransactionsForUser(householdId, userId)
    }

    suspend fun getTransactionsForUser(householdId: String, userId: String): List<TokenTransaction> {
        return tokenTransactionDao.getTransactionsForUser(householdId, userId)
    }

    fun observeBalance(householdId: String, userId: String): Flow<Int> {
        return tokenTransactionDao.observeBalance(householdId, userId)
    }

    suspend fun getBalance(householdId: String, userId: String): Int {
        return tokenTransactionDao.getBalance(householdId, userId)
    }

    fun observeEarnedThisWeek(householdId: String, userId: String): Flow<Int> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
        val end = start.atZone(zone).toLocalDate().plusDays(7).atStartOfDay(zone).toInstant()
        return tokenTransactionDao.observeEarnedBetween(householdId, userId, start, end)
    }

    suspend fun awardTaskTokensForToday(task: com.adhdfocus.app.data.model.Task, date: LocalDate) {
        if (date != LocalDate.now()) return
        val amount = task.tokenValue.coerceAtLeast(0)
        if (amount <= 0) return
        if (tokenTransactionDao.hasTaskAward(task.householdId, task.assignedUserId, task.id, date.toString())) return
        tokenTransactionDao.insert(
            TokenTransaction(
                id = "task:${task.householdId}:${task.assignedUserId}:${task.id}:${date}",
                householdId = task.householdId,
                userId = task.assignedUserId,
                amount = amount,
                type = TokenTransactionType.TASK_AWARD,
                note = "Completed: ${task.title}",
                taskId = task.id,
                targetDate = date.toString(),
                createdAt = Instant.now(),
                createdBy = "task"
            )
        )
    }

    suspend fun revokeTaskTokensForToday(task: com.adhdfocus.app.data.model.Task, date: LocalDate) {
        if (date != LocalDate.now()) return
        tokenTransactionDao.deleteTaskAward(
            householdId = task.householdId,
            userId = task.assignedUserId,
            taskId = task.id,
            targetDate = date.toString()
        )
    }

    suspend fun adjustTokens(
        householdId: String,
        userId: String,
        amount: Int,
        note: String,
        createdBy: String = "parent"
    ) {
        if (amount == 0) return
        tokenTransactionDao.insert(
            TokenTransaction(
                householdId = householdId,
                userId = userId,
                amount = amount,
                type = TokenTransactionType.PARENT_ADJUSTMENT,
                note = note.ifBlank {
                    if (amount > 0) "Parent added tokens" else "Parent removed tokens"
                },
                createdBy = createdBy
            )
        )
    }

    suspend fun redeemTokens(householdId: String, userId: String, amount: Int): Boolean {
        val redeemAmount = amount.coerceAtLeast(0)
        if (redeemAmount <= 0) return false
        val balance = getBalance(householdId, userId)
        if (redeemAmount > balance) return false
        tokenTransactionDao.insert(
            TokenTransaction(
                householdId = householdId,
                userId = userId,
                amount = -redeemAmount,
                type = TokenTransactionType.REDEMPTION,
                note = "Turned in tokens",
                createdBy = "child"
            )
        )
        return true
    }
}
