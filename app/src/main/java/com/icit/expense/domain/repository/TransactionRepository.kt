package com.icit.expense.domain.repository

import com.icit.expense.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>>
    fun getTotalSpent(): Flow<Double?>
    fun getTotalSpentByDateRange(start: Long, end: Long): Flow<Double?>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun clearAllTransactions()
    suspend fun syncFromFirestore()
}
